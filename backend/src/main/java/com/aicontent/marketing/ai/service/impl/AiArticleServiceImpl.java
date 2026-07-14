package com.aicontent.marketing.ai.service.impl;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.entity.AiGenerationTask;
import com.aicontent.marketing.ai.mapper.AiGenerationTaskMapper;
import com.aicontent.marketing.ai.parser.AiJsonParser;
import com.aicontent.marketing.ai.prompt.PromptBuilder;
import com.aicontent.marketing.ai.research.ResearchBrief;
import com.aicontent.marketing.ai.research.ResearchService;
import com.aicontent.marketing.ai.service.AiArticleService;
import com.aicontent.marketing.ai.service.AiGeneratedArticlePersistenceService;
import com.aicontent.marketing.ai.service.AiModelService;
import com.aicontent.marketing.ai.vo.AiArticleGenerateSubmitVO;
import com.aicontent.marketing.ai.vo.AiGenerationTaskVO;
import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.article.vo.ArticleDetailVO;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.product.service.ProductConfigService;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Executor;

@Service
public class AiArticleServiceImpl implements AiArticleService {

    private static final String PENDING = "PENDING";
    private static final String SEARCHING = "SEARCHING";
    private static final String WRITING = "WRITING";
    private static final String SAVING = "SAVING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final Set<String> IN_PROGRESS_STATUSES = Set.of(PENDING, SEARCHING, WRITING, SAVING);

    private final ProductConfigService productConfigService;
    private final AiModelService aiModelService;
    private final PromptBuilder promptBuilder;
    private final AiJsonParser aiJsonParser;
    private final ResearchService researchService;
    private final AiGeneratedArticlePersistenceService persistenceService;
    private final AiGenerationTaskMapper taskMapper;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;

    public AiArticleServiceImpl(
            ProductConfigService productConfigService,
            AiModelService aiModelService,
            PromptBuilder promptBuilder,
            AiJsonParser aiJsonParser,
            ResearchService researchService,
            AiGeneratedArticlePersistenceService persistenceService,
            AiGenerationTaskMapper taskMapper,
            ObjectMapper objectMapper,
            @Qualifier("aiGenerationTaskExecutor") Executor taskExecutor
    ) {
        this.productConfigService = productConfigService;
        this.aiModelService = aiModelService;
        this.promptBuilder = promptBuilder;
        this.aiJsonParser = aiJsonParser;
        this.researchService = researchService;
        this.persistenceService = persistenceService;
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public AiArticleGenerateSubmitVO generateArticle(AiArticleGenerateRequest request, Long currentUserId) {
        ProductConfigVO productConfig = findProductConfig(request);

        AiGenerationTask task = new AiGenerationTask();
        task.setUserId(currentUserId);
        task.setStatus(PENDING);
        task.setProgressMessage("任务已提交，等待联网取材");
        task.setRequestSummary(requestSummary(request));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        try {
            taskExecutor.execute(() -> process(task.getId(), currentUserId, request));
        } catch (TaskRejectedException exception) {
            fail(task.getId(), "生成任务较多，请稍后重试");
            throw new BusinessException("生成任务较多，请稍后重试");
        }
        return AiArticleGenerateSubmitVO.from(task);
    }

    @Override
    public AiGenerationTaskVO getGenerationTask(Long taskId, Long currentUserId) {
        AiGenerationTask task = taskMapper.selectById(taskId);
        if (task == null || !currentUserId.equals(task.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "生成任务不存在");
        }
        return AiGenerationTaskVO.from(task);
    }

    @PostConstruct
    public void failInterruptedTasks() {
        taskMapper.update(null, new LambdaUpdateWrapper<AiGenerationTask>()
                .in(AiGenerationTask::getStatus, IN_PROGRESS_STATUSES)
                .set(AiGenerationTask::getStatus, FAILED)
                .set(AiGenerationTask::getProgressMessage, "服务已重启")
                .set(AiGenerationTask::getErrorMessage, "服务重启导致任务中断，请重新生成")
                .set(AiGenerationTask::getUpdatedAt, LocalDateTime.now()));
    }

    private void process(Long taskId, Long currentUserId, AiArticleGenerateRequest request) {
        try {
            updateStatus(taskId, SEARCHING, "正在联网收集资料");
            ProductConfigVO productConfig = findProductConfig(request);
            ResearchBrief researchBrief = researchService.collect(productConfig, request);

            updateStatus(taskId, WRITING, "资料已准备，正在生成文章");
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(productConfig, request, researchBrief);
            String rawResult = aiModelService.chat(systemPrompt, userPrompt);
            AiGeneratedArticle generatedArticle = aiJsonParser.parseGeneratedArticle(rawResult);

            updateStatus(taskId, SAVING, "正在保存文章和资料来源");
            ArticleDetailVO article = persistenceService.save(generatedArticle, request, currentUserId, researchBrief);
            updateSuccess(taskId, article.getId());
        } catch (Exception exception) {
            fail(taskId, errorMessage(exception));
        }
    }

    private void updateStatus(Long taskId, String status, String message) {
        AiGenerationTask update = new AiGenerationTask();
        update.setId(taskId);
        update.setStatus(status);
        update.setProgressMessage(message);
        update.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    private void updateSuccess(Long taskId, Long articleId) {
        AiGenerationTask update = new AiGenerationTask();
        update.setId(taskId);
        update.setStatus(SUCCESS);
        update.setProgressMessage("文章已生成");
        update.setArticleId(articleId);
        update.setErrorMessage(null);
        update.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    private void fail(Long taskId, String message) {
        AiGenerationTask update = new AiGenerationTask();
        update.setId(taskId);
        update.setStatus(FAILED);
        update.setProgressMessage("生成失败");
        update.setErrorMessage(message);
        update.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(update);
    }

    private String requestSummary(AiArticleGenerateRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            return "{\"topic\":\"" + request.getTopic().replace("\"", "") + "\"}";
        }
    }

    private String errorMessage(Exception exception) {
        if (exception instanceof BusinessException && StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }
        return "文章生成失败，请稍后重试";
    }

    private ProductConfigVO findProductConfig(AiArticleGenerateRequest request) {
        return request.getProductConfigId() == null ? null : productConfigService.getConfig(request.getProductConfigId());
    }
}
