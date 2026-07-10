package com.aicontent.marketing.publish.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublishTaskSubmitRequest {

    private String publishType;

    private LocalDateTime scheduleTime;
}
