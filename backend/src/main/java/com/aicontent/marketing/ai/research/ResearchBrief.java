package com.aicontent.marketing.ai.research;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ResearchBrief {

    private String query;
    private LocalDateTime retrievedAt;
    private List<ResearchSourceDraft> sources = new ArrayList<>();
}
