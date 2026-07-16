package com.aicontent.marketing.publish.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.aicontent.marketing.common.security.PlatformCredentialCipher;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.mapper.PlatformAccountMapper;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.platformcontent.mapper.ArticlePlatformContentMapper;
import com.aicontent.marketing.publish.dto.PublishTaskQueryRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSaveRequest;
import com.aicontent.marketing.publish.dto.PublishTaskSubmitRequest;
import com.aicontent.marketing.publish.entity.PublishTask;
import com.aicontent.marketing.publish.mapper.PublishTaskMapper;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.publisher.PublisherRegistry;
import com.aicontent.marketing.publish.vo.PublishTaskVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_WAITING_MANUAL_CONFIRM = "WAITING_MANUAL_CONFIRM";
    private static final String PUBLISH_TYPE_IMMEDIATE = "IMMEDIATE";
    private static final String PUBLISH_TYPE_SCHEDULED = "SCHEDULED";
    private static final String ARTICLE_STATUS_FAILED = "FAILED";
    private static final String ARTICLE_STATUS_CANCELLED = "CANCELLED";

    private static final Set<String> PLATFORMS = Set.of("WECHAT_OFFICIAL", "ZHIHU", "CSDN", "JUEJIN");
    private static final Set<String> PUBLISH_TYPES = Set.of("IMMEDIATE", "SCHEDULED");
    private static final Set<String> STATUSES = Set.of(
            "DRAFT",
            "PENDING",
            "CANCELLED",
            "RUNNING",
            "SUCCESS",
            "FAILED",
            "WAITING_MANUAL_CONFIRM"
    );

    private final ArticlePlatformContentMapper platformContentMapper;
    private final PlatformAccountMapper platformAccountMapper;
    private final PublisherRegistry publisherRegistry;
    private final PlatformCredentialCipher credentialCipher;
    public PublishTaskServiceImpl(
            ArticlePlatformContentMapper platformContentMapper,
            PlatformAccountMapper platformAccountMapper,
            PublisherRegistry publisherRegistry,
            PlatformCredentialCipher credentialCipher
    ) {
        this.platformContentMapper = platformContentMapper;
        this.platformAccountMapper = platformAccountMapper;
        this.publisherRegistry = publisherRegistry;
        this.credentialCipher = credentialCipher;
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
    public void submitTask(Long id, PublishTaskSubmitRequest request, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        String publishType = normalizePublishType(request == null ? null : request.getPublishType());
        LocalDateTime scheduleTime = request == null ? null : request.getScheduleTime();
        LocalDateTime now = LocalDateTime.now();
        validateScheduleTime(publishType, scheduleTime, now);
        LambdaUpdateWrapper<PublishTask> wrapper = new LambdaUpdateWrapper<PublishTask>()
                .eq(PublishTask::getId, id)
                .eq(PublishTask::getStatus, STATUS_DRAFT)
                .set(PublishTask::getPublishType, publishType)
                .set(PublishTask::getScheduleTime, PUBLISH_TYPE_SCHEDULED.equals(publishType) ? scheduleTime : null)
                .set(PublishTask::getStatus, STATUS_PENDING)
                .set(PublishTask::getUpdatedBy, currentUserId)
                .set(PublishTask::getUpdatedAt, now);
        if (!update(wrapper)) {
            throw new BusinessException("只有草稿状态的发布任务可以提交");
        }
        task.setPublishType(publishType);
        task.setScheduleTime(PUBLISH_TYPE_SCHEDULED.equals(publishType) ? scheduleTime : null);
        task.setStatus(STATUS_PENDING);
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(now);
    }

    @Override
    @Transactional
    public void cancelTask(Long id, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        LambdaUpdateWrapper<PublishTask> wrapper = new LambdaUpdateWrapper<PublishTask>()
                .eq(PublishTask::getId, id)
                .in(PublishTask::getStatus, STATUS_DRAFT, STATUS_PENDING)
                .set(PublishTask::getStatus, STATUS_CANCELLED)
                .set(PublishTask::getArticleStatus, ARTICLE_STATUS_CANCELLED)
                .set(PublishTask::getUpdatedBy, currentUserId)
                .set(PublishTask::getUpdatedAt, LocalDateTime.now());
        if (!update(wrapper)) {
            throw new BusinessException("只有草稿或待执行状态的发布任务可以取消");
        }
        task.setStatus(STATUS_CANCELLED);
        task.setArticleStatus(ARTICLE_STATUS_CANCELLED);
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public PublishTaskVO prepareTask(Long id, Long currentUserId) {
        PublishTask task = getRequiredTask(id);
        if (!STATUS_PENDING.equals(task.getStatus())) {
            throw new BusinessException("只有待执行状态的发布任务可以执行");
        }
        if (PUBLISH_TYPE_SCHEDULED.equals(task.getPublishType())
                && task.getScheduleTime() != null
                && task.getScheduleTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("定时发布任务尚未到达计划时间");
        }
        if (!claimPendingTask(id, currentUserId)) {
            throw new BusinessException("发布任务已被其他执行器接管");
        }
        return prepareClaimedTask(getRequiredTask(id), currentUserId);
    }

    @Override
    public PublishTaskVO executeTask(Long id, Long currentUserId) {
        return prepareTask(id, currentUserId);
    }

    private boolean claimPendingTask(Long id, Long currentUserId) {
        LambdaUpdateWrapper<PublishTask> wrapper = new LambdaUpdateWrapper<PublishTask>()
                .eq(PublishTask::getId, id)
                .eq(PublishTask::getStatus, STATUS_PENDING)
                .set(PublishTask::getStatus, STATUS_RUNNING)
                .set(PublishTask::getPublishUrl, null)
                .set(PublishTask::getExternalArticleId, null)
                .set(PublishTask::getExternalPublishId, null)
                .set(PublishTask::getArticleStatus, null)
                .set(PublishTask::getErrorMessage, null)
                .set(PublishTask::getUpdatedBy, currentUserId)
                .set(PublishTask::getUpdatedAt, LocalDateTime.now());
        return update(wrapper);
    }

    private PublishTaskVO prepareClaimedTask(PublishTask task, Long currentUserId) {
        try {
            ArticlePlatformContent platformContent = getRequiredPlatformContent(task.getPlatformContentId());
            PlatformAccount account = getRequiredAccount(task.getAccountId());
            validateTaskResources(task, platformContent, account);
            PlatformPublisher publisher = publisherRegistry.getPublisher(task.getPlatform(), task.getPublishMode());
            PublishResult result = publisher.publish(toPublishContext(task, platformContent, account));
            applyResultFields(task, result);
            if (result.prepared()) {
                task.setStatus(STATUS_WAITING_MANUAL_CONFIRM);
                task.setPublishUrl(null);
                task.setArticleStatus(null);
                task.setErrorMessage(null);
            } else {
                task.setStatus(STATUS_FAILED);
                task.setPublishUrl(null);
                task.setArticleStatus(ARTICLE_STATUS_FAILED);
                task.setErrorMessage(resolveFailureMessage(result));
            }
        } catch (Exception exception) {
            task.setStatus(STATUS_FAILED);
            task.setPublishUrl(null);
            task.setArticleStatus(ARTICLE_STATUS_FAILED);
            task.setErrorMessage("发布执行失败：" + exception.getMessage());
        }
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(LocalDateTime.now());
        updateById(task);
        return toVO(task);
    }

    private void applyResultFields(PublishTask task, PublishResult result) {
        task.setPublishUrl(result.publishUrl());
        task.setExternalDraftId(result.draftId());
        task.setExternalPublishId(result.publishId());
        task.setDraftUrl(result.draftUrl());
        task.setExternalArticleId(result.articleId());
    }

    private String resolveFailureMessage(PublishResult result) {
        if (result.success() || result.submitted()) {
            return "发布准备器返回了不受支持的自动发布结果";
        }
        if (StringUtils.hasText(result.errorMessage())) {
            return result.errorMessage();
        }
        if (StringUtils.hasText(result.message())) {
            return result.message();
        }
        return "发布执行失败";
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
        String publishType = normalizePublishType(request.getPublishType());
        task.setPublishType(publishType);
        task.setScheduleTime(PUBLISH_TYPE_SCHEDULED.equals(publishType) ? request.getScheduleTime() : null);
        task.setPublishMode(resources.account().getDefaultPublishMode());
        task.setUpdatedBy(currentUserId);
        task.setUpdatedAt(now);
    }

    private ResolvedPublishResources validateAndResolve(PublishTaskSaveRequest request) {
        String publishType = normalizePublishType(request.getPublishType());
        validateScheduleTime(publishType, request.getScheduleTime(), LocalDateTime.now());

        ArticlePlatformContent platformContent = getRequiredPlatformContent(request.getPlatformContentId());
        PlatformAccount account = getRequiredAccount(request.getAccountId());
        validatePlatformConsistency(platformContent, account);
        return new ResolvedPublishResources(platformContent, account);
    }

    private String normalizePublishType(String publishType) {
        if (!StringUtils.hasText(publishType)) {
            return PUBLISH_TYPE_IMMEDIATE;
        }
        if (!PUBLISH_TYPES.contains(publishType)) {
            throw new BusinessException("publishType is invalid");
        }
        return publishType;
    }

    private void validateScheduleTime(String publishType, LocalDateTime scheduleTime, LocalDateTime now) {
        if (PUBLISH_TYPE_SCHEDULED.equals(publishType)
                && (scheduleTime == null || !scheduleTime.isAfter(now))) {
            throw new BusinessException("定时发布任务必须选择未来的发布时间");
        }
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
        if (!Integer.valueOf(1).equals(account.getEnabled())) {
            throw new BusinessException("平台账号已禁用，不能创建发布任务");
        }
    }

    private void validateTaskResources(PublishTask task, ArticlePlatformContent platformContent, PlatformAccount account) {
        if (!task.getPlatform().equals(platformContent.getPlatform())) {
            throw new BusinessException("发布任务和平台发布稿不属于同一平台");
        }
        if (!task.getPlatform().equals(account.getPlatform())) {
            throw new BusinessException("发布任务和平台账号不属于同一平台");
        }
        if (!Integer.valueOf(1).equals(account.getEnabled())) {
            throw new BusinessException("平台账号已禁用，不能执行发布任务");
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
                credentialCipher.decrypt(account.getAuthConfig()),
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
