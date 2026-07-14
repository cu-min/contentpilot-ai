package com.aicontent.marketing.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_generation_task")
public class AiGenerationTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String status;
    private String progressMessage;
    private String requestSummary;
    private Long articleId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
