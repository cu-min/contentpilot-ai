package com.aicontent.marketing.publish.service;

import com.aicontent.marketing.common.exception.BusinessException;
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
        doReturn(true).when(service).updateById(task);

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
        doReturn(true).when(service).updateById(task);

        PublishTaskSubmitRequest missingTime = new PublishTaskSubmitRequest();
        missingTime.setPublishType("SCHEDULED");
        assertThrows(BusinessException.class, () -> service.submitTask(1L, missingTime, 9L));

        PublishTaskSubmitRequest pastTime = new PublishTaskSubmitRequest();
        pastTime.setPublishType("SCHEDULED");
        pastTime.setScheduleTime(LocalDateTime.now().minusMinutes(1));
        assertThrows(BusinessException.class, () -> service.submitTask(1L, pastTime, 9L));

        verify(service, never()).updateById(task);
    }

    @Test
    void submitScheduledStoresFutureTime() {
        PublishTask task = draftTask();
        PublishTaskServiceImpl service = spy(service());
        doReturn(task).when(service).getById(1L);
        doReturn(true).when(service).updateById(task);
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

        var result = service.executeTask(1L, 9L);

        assertEquals("FAILED", result.getStatus());
        assertEquals("平台拒绝发布", result.getErrorMessage());
        assertNull(result.getPublishUrl());
    }

    private PublishTaskServiceImpl service() {
        return new PublishTaskServiceImpl(
                contentMapper,
                accountMapper,
                publisherRegistry
        );
    }

    private PublishTask draftTask() {
        PublishTask task = new PublishTask();
        task.setId(1L);
        task.setStatus("DRAFT");
        task.setCreatedBy(9L);
        return task;
    }

}
