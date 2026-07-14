package com.aicontent.marketing.ai.vo;

import com.aicontent.marketing.ai.entity.AiGenerationTask;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiGenerationTaskVO {

    private Long taskId;
    private String status;
    private String progressMessage;
    private Long articleId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AiGenerationTaskVO from(AiGenerationTask task) {
        AiGenerationTaskVO vo = new AiGenerationTaskVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setProgressMessage(task.getProgressMessage());
        vo.setArticleId(task.getArticleId());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
