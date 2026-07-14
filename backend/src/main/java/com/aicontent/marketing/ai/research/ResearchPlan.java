package com.aicontent.marketing.ai.research;

import lombok.Data;

import java.time.Instant;

@Data
public class ResearchPlan {

    private String query;
    private String officialUrl;
    private Instant startPublishedDate;
    private Instant endPublishedDate;
}
