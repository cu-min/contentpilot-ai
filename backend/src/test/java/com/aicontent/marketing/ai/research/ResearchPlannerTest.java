package com.aicontent.marketing.ai.research;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchPlannerTest {

    private final ResearchPlanner planner = new ResearchPlanner();

    @Test
    void normalTopicHasNoFixedDateFilterAndIncludesProductContext() {
        ResearchPlan plan = planner.plan(product(), request("研发团队如何选择代码审查工具", "写成面向技术负责人的文章"));

        assertNull(plan.getStartPublishedDate());
        assertNull(plan.getEndPublishedDate());
        assertEquals("https://example.com/product", plan.getOfficialUrl());
        assertTrue(plan.getQuery().contains("自动代码审查"));
        assertTrue(plan.getQuery().contains("技术负责人"));
    }

    @Test
    void topicOnlyGenerationDoesNotInventProductContext() {
        ResearchPlan plan = planner.plan(null, request("如何制定团队知识库规范", "给出可执行清单"));

        assertNull(plan.getOfficialUrl());
        assertTrue(plan.getQuery().contains("团队知识库规范"));
        assertFalse(plan.getQuery().contains("产品："));
    }

    @Test
    void freshnessTopicRestrictsSearchToSevenDays() {
        Instant before = Instant.now().minus(Duration.ofDays(7).plusSeconds(2));
        ResearchPlan plan = planner.plan(product(), request("本周 AI 代码审查新闻", null));

        assertNotNull(plan.getStartPublishedDate());
        assertTrue(plan.getStartPublishedDate().isAfter(before));
        assertNull(plan.getEndPublishedDate());
    }

    @Test
    void explicitDateRangeOverridesFreshnessWindow() {
        ResearchPlan plan = planner.plan(product(), request("2025-03-01 到 2025-03-31 的行业新闻", "请使用这个时间范围"));

        assertEquals(Instant.parse("2025-02-28T16:00:00Z"), plan.getStartPublishedDate());
        assertEquals(Instant.parse("2025-03-31T16:00:00Z"), plan.getEndPublishedDate());
    }

    private ProductConfigVO product() {
        ProductConfigVO product = new ProductConfigVO();
        product.setProductName("CodeReview Pro");
        product.setOfficialUrl("https://example.com/product");
        product.setCoreFeatures("自动代码审查");
        product.setTargetUsers("技术负责人");
        product.setAdvantages("减少人工审查压力");
        return product;
    }

    private AiArticleGenerateRequest request(String topic, String extraRequirement) {
        AiArticleGenerateRequest request = new AiArticleGenerateRequest();
        request.setTopic(topic);
        request.setType("INDUSTRY_KNOWLEDGE");
        request.setLanguage("ZH");
        request.setExtraRequirement(extraRequirement);
        return request;
    }
}
