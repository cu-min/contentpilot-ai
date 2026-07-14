package com.aicontent.marketing.ai.research;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResearchSourceDraft {

    private String sourceType;
    private String title;
    private String url;
    private String domain;
    private LocalDateTime publishedAt;
    private String excerpt;
}
