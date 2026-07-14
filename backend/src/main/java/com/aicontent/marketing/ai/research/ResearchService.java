package com.aicontent.marketing.ai.research;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ResearchService {

    private static final int MAX_SOURCES = 5;

    private final ResearchPlanner researchPlanner;
    private final ExaResearchClient exaResearchClient;

    public ResearchService(ResearchPlanner researchPlanner, ExaResearchClient exaResearchClient) {
        this.researchPlanner = researchPlanner;
        this.exaResearchClient = exaResearchClient;
    }

    public ResearchBrief collect(ProductConfigVO productConfig, AiArticleGenerateRequest request) {
        ResearchPlan plan = researchPlanner.plan(productConfig, request);
        List<ResearchSourceDraft> collected = new ArrayList<>();
        ResearchSourceDraft officialSource = exaResearchClient.fetchOfficialSource(plan.getOfficialUrl());
        if (officialSource != null) {
            collected.add(officialSource);
        }
        collected.addAll(exaResearchClient.search(plan));

        List<ResearchSourceDraft> sources = selectSources(collected);
        if (sources.isEmpty()) {
            throw new BusinessException("Exa 未返回可用资料来源，请调整主题后重试");
        }
        ResearchBrief brief = new ResearchBrief();
        brief.setQuery(plan.getQuery());
        brief.setRetrievedAt(LocalDateTime.now());
        brief.setSources(sources);
        return brief;
    }

    private List<ResearchSourceDraft> selectSources(List<ResearchSourceDraft> candidates) {
        List<ResearchSourceDraft> selected = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        for (ResearchSourceDraft candidate : candidates) {
            if (candidate == null || !StringUtils.hasText(candidate.getUrl()) || !seenUrls.add(canonicalUrl(candidate.getUrl()))) {
                continue;
            }
            if (!StringUtils.hasText(candidate.getTitle()) || !StringUtils.hasText(candidate.getExcerpt())) {
                continue;
            }
            selected.add(candidate);
            if (selected.size() == MAX_SOURCES) {
                break;
            }
        }
        return selected;
    }

    private String canonicalUrl(String url) {
        try {
            URI uri = URI.create(url);
            return (uri.getHost() == null ? "" : uri.getHost().toLowerCase())
                    + (uri.getPath() == null ? "" : uri.getPath());
        } catch (IllegalArgumentException exception) {
            return url;
        }
    }
}
