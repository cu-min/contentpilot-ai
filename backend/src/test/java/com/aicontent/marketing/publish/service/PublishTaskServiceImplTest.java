package com.aicontent.marketing.publish.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platform.mapper.PlatformAccountMapper;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.platformcontent.mapper.ArticlePlatformContentMapper;
import com.aicontent.marketing.publish.dto.PublishTaskSubmitRequest;
import com.aicontent.marketing.publish.entity.PublishTask;
import com.aicontent.marketing.publish.mapper.PublishTaskMapper;
import com.aicontent.marketing.publish.publisher.PublisherRegistry;
import com.aicontent.marketing.publish.publisher.juejin.JuejinClient;
import com.aicontent.marketing.publish.publisher.wechat.WechatAccessTokenCache;
import com.aicontent.marketing.publish.publisher.wechat.WechatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

class PublishTaskServiceImplTest {

    private final PublishTaskMapper taskMapper = mock(PublishTaskMapper.class);
    private final ArticlePlatformContentMapper contentMapper = mock(ArticlePlatformContentMapper.class);
    private final PlatformAccountMapper accountMapper = mock(PlatformAccountMapper.class);
    private final PublisherRegistry publisherRegistry = mock(PublisherRegistry.class);
    private final JuejinClient juejinClient = mock(JuejinClient.class);
    private final WechatClient wechatClient = mock(WechatClient.class);
    private final WechatAccessTokenCache tokenCache = mock(WechatAccessTokenCache.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private PublishTaskServiceImpl service() {
        return new PublishTaskServiceImpl(
                contentMapper,
                accountMapper,
                publisherRegistry,
                juejinClient,
                wechatClient,
                tokenCache,
                objectMapper
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
