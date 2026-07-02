package com.aicontent.marketing.article.dto;

import lombok.Data;

@Data
public class ArticleQueryRequest {

    private long page = 1;

    private long size = 10;

    private String keyword;

    private String status;

    private String type;

    private String language;
}
