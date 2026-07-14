package com.aicontent.marketing.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleUpdateRequest {

    private Long productConfigId;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be less than 200 characters")
    private String title;

    @Size(max = 500, message = "summary must be less than 500 characters")
    private String summary;

    private String content;

    @NotBlank(message = "type is required")
    @Pattern(
            regexp = "PRODUCT_INTRO|TUTORIAL|INDUSTRY_KNOWLEDGE|COMPARISON|SOLUTION|SEO",
            message = "type is invalid"
    )
    private String type;

    @NotBlank(message = "language is required")
    @Pattern(regexp = "ZH|EN", message = "language is invalid")
    private String language;

    @Size(max = 500, message = "tags must be less than 500 characters")
    private String tags;

    @Size(max = 500, message = "keywords must be less than 500 characters")
    private String keywords;
}
