package com.aicontent.marketing.ai.service.impl;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.parser.AiJsonParser;
import com.aicontent.marketing.ai.prompt.PromptBuilder;
import com.aicontent.marketing.ai.service.AiArticleService;
import com.aicontent.marketing.ai.service.AiModelService;
import com.aicontent.marketing.ai.vo.AiArticleGenerateVO;
import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.article.dto.ArticleCreateRequest;
import com.aicontent.marketing.article.service.ArticleService;
import com.aicontent.marketing.article.vo.ArticleDetailVO;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.product.service.ProductConfigService;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiArticleServiceImpl implements AiArticleService {

    private final ProductConfigService productConfigService;
    private final ArticleService articleService;
    private final AiModelService aiModelService;
    private final PromptBuilder promptBuilder;
    private final AiJsonParser aiJsonParser;

    public AiArticleServiceImpl(
            ProductConfigService productConfigService,
            ArticleService articleService,
            AiModelService aiModelService,
            PromptBuilder promptBuilder,
            AiJsonParser aiJsonParser
    ) {
        this.productConfigService = productConfigService;
        this.articleService = articleService;
        this.aiModelService = aiModelService;
        this.promptBuilder = promptBuilder;
        this.aiJsonParser = aiJsonParser;
    }

    @Override
    public AiArticleGenerateVO generateArticle(AiArticleGenerateRequest request, Long currentUserId) {
        ProductConfigVO productConfig = productConfigService.getCurrentConfig();
        if (!StringUtils.hasText(productConfig.getProductName())) {
            throw new BusinessException("请先完成产品配置，再生成文章");
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(productConfig, request);
        String rawResult = aiModelService.chat(systemPrompt, userPrompt);
        AiGeneratedArticle generatedArticle = aiJsonParser.parseGeneratedArticle(rawResult);

        ArticleCreateRequest articleRequest = new ArticleCreateRequest();
        articleRequest.setTitle(requiredText(generatedArticle.getTitle(), "AI 返回标题为空，请重试"));
        articleRequest.setSummary(generatedArticle.getSummary());
        articleRequest.setContent(requiredText(generatedArticle.getContent(), "AI 返回正文为空，请重试"));
        articleRequest.setType(request.getType());
        articleRequest.setLanguage(request.getLanguage());
        articleRequest.setTags(join(generatedArticle.getTags()));
        articleRequest.setKeywords(join(generatedArticle.getKeywords()));

        ArticleDetailVO article = articleService.createArticle(articleRequest, currentUserId);
        return AiArticleGenerateVO.from(article);
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }
}
