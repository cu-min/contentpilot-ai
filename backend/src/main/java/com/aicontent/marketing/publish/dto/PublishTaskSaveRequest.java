package com.aicontent.marketing.publish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublishTaskSaveRequest {

    @NotNull(message = "请选择平台发布稿")
    private Long platformContentId;

    @NotNull(message = "请选择平台账号")
    private Long accountId;

    @Size(max = 200, message = "任务标题最多 200 个字符")
    private String title;

    @NotBlank(message = "请选择发布类型")
    private String publishType;

    private LocalDateTime scheduleTime;
}
