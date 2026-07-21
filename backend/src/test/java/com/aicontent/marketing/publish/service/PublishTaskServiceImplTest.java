package com.aicontent.marketing.publish.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.security.PlatformCredentialCipher;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.mapper.PlatformAccountMapper;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.platformcontent.mapper.ArticlePlatformContentMapper;
import com.aicontent.marketing.publish.dto.PublishTaskSubmitRequest;
import com.aicontent.marketing.publish.entity.PublishTask;
import com.aicontent.marketing.publish.mapper.PublishTaskMapper;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.publisher.PublisherRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PublishTaskServiceImplTest {

    private final PublishTaskMapper taskMapper = mock(PublishTaskMapper.class);
    private final ArticlePlatformContentMapper contentMapper = mock(ArticlePlatformContentMapper.class);
    private final PlatformAccountMapper accountMapper = mock(PlatformAccountMapper.class);
    private final PublisherRegistry publisherRegistry = mock(PublisherRegistry.class);

    @BeforeAll
    static void initializePublishTaskMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PublishTask.class);
    }

    @Test
    void submitWithoutPayloadDefaultsToImmediateAndClearsScheduleTime() {
        PublishTask task = draftTask();
        task.setPublishType("SCHEDULED");
        task.setScheduleTime(LocalDateTime.now().plusHours(1));
        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

        service.submitTask(1L, null, 9L);

        assertEquals("PENDING", task.getStatus());
        assertEquals("IMMEDIATE", task.getPublishType());
        assertNull(task.getScheduleTime());
    }

    @Test
    void submitScheduledRejectsMissingOrPastTime() {
        PublishTask task = draftTask();
        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

        PublishTaskSubmitRequest missingTime = new PublishTaskSubmitRequest();
        missingTime.setPublishType("SCHEDULED");
        assertThrows(BusinessException.class, () -> service.submitTask(1L, missingTime, 9L));

        PublishTaskSubmitRequest pastTime = new PublishTaskSubmitRequest();
        pastTime.setPublishType("SCHEDULED");
        pastTime.setScheduleTime(LocalDateTime.now().minusMinutes(1));
        assertThrows(BusinessException.class, () -> service.submitTask(1L, pastTime, 9L));

        verify(service, never()).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    void submitScheduledStoresFutureTime() {
        PublishTask task = draftTask();
        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        LocalDateTime scheduleTime = LocalDateTime.now().plusMinutes(5);
        PublishTaskSubmitRequest request = new PublishTaskSubmitRequest();
        request.setPublishType("SCHEDULED");
        request.setScheduleTime(scheduleTime);

        service.submitTask(1L, request, 9L);

        assertEquals("PENDING", task.getStatus());
        assertEquals("SCHEDULED", task.getPublishType());
        assertEquals(scheduleTime, task.getScheduleTime());
    }

    @Test
    void createTaskRejectsDisabledAccount() {
        ArticlePlatformContent content = new ArticlePlatformContent();
        content.setId(2L);
        content.setArticleId(3L);
        content.setPlatform("JUEJIN");
        PlatformAccount account = new PlatformAccount();
        account.setId(4L);
        account.setPlatform("JUEJIN");
        account.setEnabled(0);
        when(contentMapper.selectById(2L)).thenReturn(content);
        when(accountMapper.selectById(4L)).thenReturn(account);

        var request = new com.aicontent.marketing.publish.dto.PublishTaskSaveRequest();
        request.setPlatformContentId(2L);
        request.setAccountId(4L);

        assertThrows(BusinessException.class, () -> service().createTask(request, 9L));
    }

    @Test
    void executionFailureAlwaysEndsInFailedStatusWithReason() {
        PublishTask task = new PublishTask();
        task.setId(1L);
        task.setStatus("PENDING");
        task.setPublishType("IMMEDIATE");
        task.setPlatform("JUEJIN");
        task.setPublishMode("UNOFFICIAL_API");
        task.setPlatformContentId(2L);
        task.setAccountId(4L);
        task.setTitle("标题");

        ArticlePlatformContent content = new ArticlePlatformContent();
        content.setId(2L);
        content.setArticleId(3L);
        content.setPlatform("JUEJIN");
        content.setContent("正文");
        PlatformAccount account = new PlatformAccount();
        account.setId(4L);
        account.setPlatform("JUEJIN");
        account.setEnabled(1);
        account.setDefaultPublishMode("UNOFFICIAL_API");

        PlatformPublisher publisher = mock(PlatformPublisher.class);
        when(publisherRegistry.getPublisher("JUEJIN", "UNOFFICIAL_API")).thenReturn(publisher);
        when(publisher.publish(any())).thenReturn(PublishResult.failed("平台拒绝发布"));
        when(contentMapper.selectById(2L)).thenReturn(content);
        when(accountMapper.selectById(4L)).thenReturn(account);

        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        doReturn(true).when(service).updateById(task);

        var result = service.prepareTask(1L, 9L);

        assertEquals("FAILED", result.getStatus());
        assertEquals("平台拒绝发布", result.getErrorMessage());
        assertNull(result.getPublishUrl());
    }

    @Test
    void preparationPersistsDraftFieldsAndWaitsForManualConfirmation() {
        PublishTask task = executableTask();
        ArticlePlatformContent content = platformContent();
        PlatformAccount account = enabledAccount();
        PlatformPublisher publisher = mock(PlatformPublisher.class);
        when(publisherRegistry.getPublisher("JUEJIN", "UNOFFICIAL_API")).thenReturn(publisher);
        when(publisher.publish(any())).thenReturn(PublishResult.prepared(
                "draft-123",
                "https://juejin.cn/editor/drafts/draft-123",
                "草稿已准备"
        ));
        when(contentMapper.selectById(2L)).thenReturn(content);
        when(accountMapper.selectById(4L)).thenReturn(account);

        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        doReturn(true).when(service).updateById(task);

        var result = service.prepareTask(1L, 9L);

        assertEquals("WAITING_MANUAL_CONFIRM", result.getStatus());
        assertEquals("draft-123", result.getExternalDraftId());
        assertEquals("https://juejin.cn/editor/drafts/draft-123", result.getDraftUrl());
        assertNull(result.getPublishUrl());
        assertNull(result.getArticleStatus());
        assertNull(result.getErrorMessage());
    }

    @Test
    void automaticSuccessResultIsRejectedByPreparationWorkflow() {
        PublishTask task = executableTask();
        PlatformPublisher publisher = mock(PlatformPublisher.class);
        when(publisherRegistry.getPublisher("JUEJIN", "UNOFFICIAL_API")).thenReturn(publisher);
        when(publisher.publish(any())).thenReturn(PublishResult.success("https://juejin.cn/post/123", "unexpected"));
        when(contentMapper.selectById(2L)).thenReturn(platformContent());
        when(accountMapper.selectById(4L)).thenReturn(enabledAccount());
        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        doReturn(true).when(service).updateById(task);

        var result = service.prepareTask(1L, 9L);

        assertEquals("FAILED", result.getStatus());
        assertEquals("发布准备器返回了不受支持的自动发布结果", result.getErrorMessage());
    }

    @Test
    void preparationDecryptsAccountConfigBeforeCallingPublisher() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(key);
        String plaintext = "{\"cookie\":\"sid=ok\"}";
        PublishTask task = executableTask();
        PlatformAccount account = enabledAccount();
        account.setAuthConfig(cipher.encrypt(plaintext));
        PlatformPublisher publisher = mock(PlatformPublisher.class);
        when(publisherRegistry.getPublisher("JUEJIN", "UNOFFICIAL_API")).thenReturn(publisher);
        when(publisher.publish(any())).thenAnswer(invocation -> {
            var context = (com.aicontent.marketing.publish.publisher.PublishContext) invocation.getArgument(0);
            assertEquals(plaintext, context.accountAuthConfig());
            return PublishResult.prepared("draft-123", "https://juejin.cn/editor/drafts/draft-123", "ready");
        });
        when(contentMapper.selectById(2L)).thenReturn(platformContent());
        when(accountMapper.selectById(4L)).thenReturn(account);
        PublishTaskServiceImpl service = spy(service(cipher));
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));
        doReturn(true).when(service).updateById(task);

        assertEquals("WAITING_MANUAL_CONFIRM", service.prepareTask(1L, 9L).getStatus());
    }

    @Test
    void submitFailsWhenAtomicDraftTransitionLosesRace() {
        PublishTaskServiceImpl service = spy(service());
        doReturn(draftTask()).when(service).getById(1L);
        doReturn(false).when(service).update(any(LambdaUpdateWrapper.class));

        assertThrows(BusinessException.class, () -> service.submitTask(1L, null, 9L));
    }

    private PublishTaskServiceImpl service() {
        return service(new PlatformCredentialCipher(""));
    }

    private PublishTaskServiceImpl service(PlatformCredentialCipher cipher) {
        return new PublishTaskServiceImpl(
                contentMapper,
                accountMapper,
                publisherRegistry,
                cipher
        );
    }

    private PublishTask draftTask() {
        PublishTask task = new PublishTask();
        task.setId(1L);
        task.setStatus("DRAFT");
        task.setCreatedBy(9L);
        return task;
    }

    private PublishTask executableTask() {
        PublishTask task = new PublishTask();
        task.setId(1L);
        task.setArticleId(3L);
        task.setStatus("PENDING");
        task.setPublishType("IMMEDIATE");
        task.setPlatform("JUEJIN");
        task.setPublishMode("UNOFFICIAL_API");
        task.setPlatformContentId(2L);
        task.setAccountId(4L);
        task.setTitle("标题");
        return task;
    }

    private ArticlePlatformContent platformContent() {
        ArticlePlatformContent content = new ArticlePlatformContent();
        content.setId(2L);
        content.setArticleId(3L);
        content.setPlatform("JUEJIN");
        content.setContent("正文");
        return content;
    }

    private PlatformAccount enabledAccount() {
        PlatformAccount account = new PlatformAccount();
        account.setId(4L);
        account.setPlatform("JUEJIN");
        account.setEnabled(1);
        account.setDefaultPublishMode("UNOFFICIAL_API");
        return account;
    }

}
