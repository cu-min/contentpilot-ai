package com.aicontent.marketing.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiGeneratedArticle {

    private String title;

    private String summary;

    private String content;

    private List<String> tags;

    private List<String> keywords;
}
