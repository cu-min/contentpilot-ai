package com.aicontent.marketing.publish.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.mapper.PlatformAccountMapper;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.platformcontent.mapper.ArticlePlatformContentMapper;
import com.aicontent.marketing.publish.dto.PublishTaskQueryRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSaveRequest;
import com.aicontent.marketing.publish.entity.PublishTask;
import com.aicontent.marketing.publish.mapper.PublishTaskMapper;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.publisher.PublisherRegistry;
import com.aicontent.marketing.publish.vo.PublishTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class PublishTaskServiceImpl extends ServiceImpl<PublishTaskMapper, PublishTask>
        implements PublishTaskService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final Set<String> PLATFORMS = Set.of("WECHAT_OFFICIAL", "ZHIHU", "CSDN", "JUEJIN");
    private static final Set<String> PUBLISH_TYPES = Set.of("IMMEDIATE", "SCHEDULED");
    private static final Set<String> STATUSES = Set.of(
            "DRAFT",
            "PENDING",
            "CANCELLED",
            "RUNNING",
            "SUCCESS",
            "FAILED",
            "NEED_LOGIN",
            "NEED_CAPTCHA",
            "NEED_MANUAL_CONFIRM",
            "LINK_FETCH_FAILED",
            "CONTENT_REJECTED"
    );

    private final ArticlePlatformContentMapper platformContentMapper;
    private final PlatformAccountMapper platformAccountMapper;
    private final PublisherRegistry publisherRegistry;

    public PublishTaskServiceImpl(
            ArticlePlatformContentMapper platformContentMapper,
            PlatformAccountMapper platformAccountMapper,
            PublisherRegistry publisherRegistry
    ) {
        this.platformContentMapper = platformContentMapper;
        this.platformAccountMapper = platformAccountMapper;
        this.publisherRegistry = publisherRegistry;
    }

    @Override
    public Page<PublishTaskVO> listTasks(PublishTaskQueryRequest request) {
        validateOptionalFilters(request);
        Page<PublishTask> taskPage = new Page<>(normalizePage(request.getPage()), normalizeSize(request.getSize()));
        LambdaQueryWrapper<PublishTask> wrapper = new LambdaQueryWrapper<PublishTask>()
                .orderByDesc(PublishTask::getUpdatedAt)
                .orderByDesc(PublishTask::getId);

        if (StringUtils.hasText(request.getPlatform())) {
            wrapper.eq(PublishTask::getPlatform, request.getPlatform());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(PublishTask::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getPublishType())) {
            wrapper.eq(PublishTask::getPublishType, request.getPublishType());
        }

        Page<PublishTask> result = page(taskPage, wrapper);
        Page<PublishTaskVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public PublishTaskVO getTaskDetail(Long id) {
        return toVO(getRequiredTask(id));
    }

    @Override
    @Transactional
    public PublishTaskVO createTask(PublishTaskSaveRequest request, Long currentUserId) {
        ResolvedPublishResources resources = validateAndResolve(request);
        LocalDateTime now = LocalDateTime.now();
        PublishTask task = new PublishTask();
        task.setStatus(STATUS_DRAFT);
        task.setCreatedBy(currentUserId);
        task.setCreatedAt(now);
        fillTask(task, request, resources, currentUserId, now);
        save(task);
        return toVO(task);
    }

    @Override
    @Transactional
    public PublishTaskVO updateTask(Long id, PublishTaskSaveRequest request, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        if (!STATUS_DRAFT.equals(task.getStatus())) {
            throw new BusinessException("只有草稿状态的发布任务可以编辑");
        }
        ResolvedPublishResources resources = validateAndResolve(request);
        fillTask(task, request, resources, currentUserId, LocalDateTime.now());
        updateById(task);
        return toVO(task);
    }

    @Override
    @Transactional
    public void submitTask(Long id, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        if (!STATUS_DRAFT.equals(task.getStatus())) {
            throw new BusinessException("只有草稿状态的发布任务可以提交");
        }
        task.setStatus(STATUS_PENDING);
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(LocalDateTime.now());
        updateById(task);
    }

    @Override
    @Transactional
    public void cancelTask(Long id, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        if (!STATUS_DRAFT.equals(task.getStatus()) && !STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException("只有草稿或待执行状态的发布任务可以取消");
        }
        task.setStatus(STATUS_CANCELLED);
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(LocalDateTime.now());
        updateById(task);
    }

    @Override
    public PublishTaskVO executeTask(Long id, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        if (!STATUS_PENDING.equals(task.getStatus()) && !STATUS_FAILED.equals(task.getStatus())) {
            throw new BusinessException("只有待执行或执行失败的发布任务可以执行");
        }

        ArticlePlatformContent platformContent = getRequiredPlatformContent(task.getPlatformContentId());
        PlatformAccount account = getRequiredAccount(task.getAccountId());
        validateTaskResources(task, platformContent, account);

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(STATUS_RUNNING);
        task.setPublishUrl(null);
        task.setExternalArticleId(null);
        task.setErrorMessage(null);
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(now);
        updateById(task);

        PlatformPublisher publisher = publisherRegistry.getPublisher(task.getPlatform(), task.getPublishMode());
        try {
            PublishResult result = publisher.publish(toPublishContext(task, platformContent, account));
            fillExternalResult(task, result);
            if (result.success()) {
                task.setStatus(STATUS_SUCCESS);
                task.setPublishUrl(result.publishUrl());
                task.setErrorMessage(null);
            } else {
                task.setStatus(STATUS_FAILED);
                task.setPublishUrl(null);
                task.setErrorMessage(StringUtils.hasText(result.errorMessage()) ? result.errorMessage() : "发布执行失败");
            }
        } catch (Exception exception) {
            task.setStatus(STATUS_FAILED);
            task.setPublishUrl(null);
            task.setErrorMessage("发布执行失败：" + exception.getMessage());
        }
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(LocalDateTime.now());
        updateById(task);
        return toVO(task);
    }

    private void fillExternalResult(PublishTask task, PublishResult result) {
        if (StringUtils.hasText(result.draftId())) {
            task.setExternalDraftId(result.draftId());
        }
        if (StringUtils.hasText(result.draftUrl())) {
            task.setDraftUrl(result.draftUrl());
        }
        if (StringUtils.hasText(result.articleId())) {
            task.setExternalArticleId(result.articleId());
        }
    }

    private void fillTask(
            PublishTask task,
            PublishTaskSaveRequest request,
            ResolvedPublishResources resources,
            Long currentUserId,
            LocalDateTime now
    ) {
        task.setArticleId(resources.platformContent().getArticleId());
        task.setPlatformContentId(resources.platformContent().getId());
        task.setPlatform(resources.platformContent().getPlatform());
        task.setAccountId(resources.account().getId());
        task.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle() : resources.platformContent().getTitle());
        task.setPublishType(request.getPublishType());
        task.setScheduleTime("SCHEDULED".equals(request.getPublishType()) ? request.getScheduleTime() : null);
        task.setPublishMode(resources.account().getDefaultPublishMode());
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(now);
    }

    private ResolvedPublishResources validateAndResolve(PublishTaskSaveRequest request) {
        if (!PUBLISH_TYPES.contains(request.getPublishType())) {
            throw new BusinessException("publishType is invalid");
        }
        if ("SCHEDULED".equals(request.getPublishType()) && request.getScheduleTime() == null) {
            throw new BusinessException("定时发布任务必须选择发布时间");
        }

        ArticlePlatformContent platformContent = getRequiredPlatformContent(request.getPlatformContentId());
        PlatformAccount account = getRequiredAccount(request.getAccountId());
        validatePlatformConsistency(platformContent, account);
        return new ResolvedPublishResources(platformContent, account);
    }

    private ArticlePlatformContent getRequiredPlatformContent(Long id) {
        ArticlePlatformContent platformContent = platformContentMapper.selectById(id);
        if (platformContent == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "平台发布稿不存在");
        }
        return platformContent;
    }

    private PlatformAccount getRequiredAccount(Long id) {
        PlatformAccount account = platformAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "平台账号不存在");
        }
        return account;
    }

    private void validatePlatformConsistency(ArticlePlatformContent platformContent, PlatformAccount account) {
        if (!platformContent.getPlatform().equals(account.getPlatform())) {
            throw new BusinessException("平台发布稿和平台账号不属于同一平台");
        }
    }

    private void validateTaskResources(PublishTask task, ArticlePlatformContent platformContent, PlatformAccount account) {
        if (!task.getPlatform().equals(platformContent.getPlatform())) {
            throw new BusinessException("发布任务和平台发布稿不属于同一平台");
        }
        if (!task.getPlatform().equals(account.getPlatform())) {
            throw new BusinessException("发布任务和平台账号不属于同一平台");
        }
    }

    private PublishContext toPublishContext(PublishTask task, ArticlePlatformContent content, PlatformAccount account) {
        return new PublishContext(
                task.getId(),
                task.getArticleId(),
                task.getPlatformContentId(),
                task.getPlatform(),
                task.getAccountId(),
                task.getTitle(),
                content.getContent(),
                content.getSummary(),
                content.getTags(),
                content.getKeywords(),
                task.getPublishMode(),
                task.getExternalDraftId(),
                task.getExternalArticleId(),
                task.getDraftUrl(),
                StringUtils.hasText(account.getAuthConfig()),
                account.getAuthConfig(),
                account.getAccountName(),
                account.getRemark(),
                task.getScheduleTime()
        );
    }

    private PublishTask getRequiredTask(Long id) {
        PublishTask task = getById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发布任务不存在");
        }
        return task;
    }

    private PublishTaskVO toVO(PublishTask task) {
        ArticlePlatformContent content = platformContentMapper.selectById(task.getPlatformContentId());
        PlatformAccount account = platformAccountMapper.selectById(task.getAccountId());
        return PublishTaskVO.from(task, content, account);
    }

    private void validateOptionalFilters(PublishTaskQueryRequest request) {
        if (StringUtils.hasText(request.getPlatform()) && !PLATFORMS.contains(request.getPlatform())) {
            throw new BusinessException("platform is invalid");
        }
        if (StringUtils.hasText(request.getStatus()) && !STATUSES.contains(request.getStatus())) {
            throw new BusinessException("status is invalid");
        }
        if (StringUtils.hasText(request.getPublishType()) && !PUBLISH_TYPES.contains(request.getPublishType())) {
            throw new BusinessException("publishType is invalid");
        }
    }

    private long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private record ResolvedPublishResources(ArticlePlatformContent platformContent, PlatformAccount account) {
    }
}
