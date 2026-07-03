package com.aicontent.marketing.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiArticleGenerateRequest {

    @NotBlank(message = "topic is required")
    @Size(max = 200, message = "topic must be less than 200 characters")
    private String topic;

    @NotBlank(message = "type is required")
    @Pattern(
            regexp = "PRODUCT_INTRO|TUTORIAL|INDUSTRY_KNOWLEDGE|COMPARISON|SOLUTION|SEO",
            message = "type is invalid"
    )
    private String type;

    @NotBlank(message = "language is required")
    @Pattern(regexp = "ZH|EN", message = "language is invalid")
    private String language;

    @Size(max = 1000, message = "extraRequirement must be less than 1000 characters")
    private String extraRequirement;
}
