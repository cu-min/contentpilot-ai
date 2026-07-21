package com.aicontent.marketing.ai.service.impl;

import com.aicontent.marketing.ai.entity.AiGenerationTask;
import com.aicontent.marketing.ai.mapper.AiGenerationTaskMapper;
import com.aicontent.marketing.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiArticleServiceImplTest {

    @Mock
    private AiGenerationTaskMapper taskMapper;

    @InjectMocks
    private AiArticleServiceImpl service;

    // ==================== getGenerationTask ====================

    @Test
    void returnsTaskWhenFoundAndOwnedByUser() {
        AiGenerationTask task = new AiGenerationTask();
        task.setId(1L);
        task.setUserId(100L);
        task.setStatus("SUCCESS");
        when(taskMapper.selectById(1L)).thenReturn(task);

        var result = service.getGenerationTask(1L, 100L);

        assertEquals(1L, result.getTaskId());
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void throwsWhenTaskNotFound() {
        when(taskMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getGenerationTask(1L, 100L));

        assertEquals("生成任务不存在", ex.getMessage());
    }

    @Test
    void throwsWhenTaskBelongsToOtherUser() {
        AiGenerationTask task = new AiGenerationTask();
        task.setId(1L);
        task.setUserId(200L);
        when(taskMapper.selectById(1L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getGenerationTask(1L, 100L));

        assertEquals("生成任务不存在", ex.getMessage());
    }

    // ==================== errorMessage ====================

    @Test
    void errorMessageReturnsBusinessExceptionMessage() {
        BusinessException ex = new BusinessException("AI 接口超时");

        String result = invokeErrorMessage(ex);

        assertEquals("AI 接口超时", result);
    }

    @Test
    void errorMessageReturnsFallbackForRegularException() {
        String result = invokeErrorMessage(new RuntimeException("连接超时"));

        assertEquals("文章生成失败，请稍后重试", result);
    }

    @Test
    void errorMessageReturnsFallbackWhenBusinessExceptionHasNoMessage() {
        BusinessException ex = new BusinessException((String) null);

        String result = invokeErrorMessage(ex);

        assertEquals("文章生成失败，请稍后重试", result);
    }

    // ==================== reflection helper ====================

    private String invokeErrorMessage(Exception exception) {
        return (String) ReflectionTestUtils.invokeMethod(service,
                "errorMessage", exception);
    }
}
