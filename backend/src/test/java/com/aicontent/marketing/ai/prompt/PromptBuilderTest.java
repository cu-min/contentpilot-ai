package com.aicontent.marketing.ai.prompt;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.research.ResearchBrief;
import com.aicontent.marketing.ai.research.ResearchSourceDraft;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void includesEditorialGuardrailsAndStructuredResearchContext() {
        ProductConfigVO product = new ProductConfigVO();
        product.setProductName("Figma");
        product.setCoreFeatures("协作设计和开发交付");
        AiArticleGenerateRequest request = new AiArticleGenerateRequest();
        request.setTopic("设计系统如何减少交付返工");
        request.setType("SOLUTION");
        request.setLanguage("ZH");
        ResearchSourceDraft source = new ResearchSourceDraft();
        source.setTitle("官方资料");
        source.setDomain("figma.com");
        source.setUrl("https://www.figma.com/");
        source.setExcerpt("设计与开发共享上下文。");
        ResearchBrief brief = new ResearchBrief();
        brief.setSources(List.of(source));

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(product, request, brief);

        assertTrue(systemPrompt.contains("每一段都必须承担一个作用"));
        assertTrue(systemPrompt.contains("不要编造亲身经历"));
        assertTrue(userPrompt.contains("场景 → 原因/机制 → 可执行做法或取舍"));
        assertTrue(userPrompt.contains("figma.com"));
        assertTrue(userPrompt.contains("官方资料"));
    }

    @Test
    void topicOnlyGenerationDoesNotAssumeAProduct() {
        AiArticleGenerateRequest request = new AiArticleGenerateRequest();
        request.setTopic("如何建立团队知识库规范");
        request.setType("INDUSTRY_KNOWLEDGE");
        request.setLanguage("ZH");

        String userPrompt = promptBuilder.buildUserPrompt(null, request, new ResearchBrief());

        assertTrue(userPrompt.contains("本次未选择产品"));
        assertTrue(userPrompt.contains("不要假设任何特定品牌"));
    }
}
