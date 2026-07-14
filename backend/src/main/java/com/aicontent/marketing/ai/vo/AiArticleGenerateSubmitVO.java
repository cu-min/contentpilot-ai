package com.aicontent.marketing.ai.vo;

import com.aicontent.marketing.ai.entity.AiGenerationTask;
import lombok.Data;

@Data
public class AiArticleGenerateSubmitVO {

    private Long taskId;
    private String status;

    public static AiArticleGenerateSubmitVO from(AiGenerationTask task) {
        AiArticleGenerateSubmitVO vo = new AiArticleGenerateSubmitVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        return vo;
    }
}
