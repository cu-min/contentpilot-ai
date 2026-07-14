package com.aicontent.marketing.ai.service.impl;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.research.ResearchBrief;
import com.aicontent.marketing.ai.research.ResearchSourceDraft;
import com.aicontent.marketing.ai.service.AiGeneratedArticlePersistenceService;
import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.article.dto.ArticleCreateRequest;
import com.aicontent.marketing.article.entity.ArticleResearchSource;
import com.aicontent.marketing.article.mapper.ArticleResearchSourceMapper;
import com.aicontent.marketing.article.service.ArticleService;
import com.aicontent.marketing.article.vo.ArticleDetailVO;
import com.aicontent.marketing.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiGeneratedArticlePersistenceServiceImpl implements AiGeneratedArticlePersistenceService {

    private final ArticleService articleService;
    private final ArticleResearchSourceMapper sourceMapper;

    public AiGeneratedArticlePersistenceServiceImpl(ArticleService articleService,
                                                    ArticleResearchSourceMapper sourceMapper) {
        this.articleService = articleService;
        this.sourceMapper = sourceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleDetailVO save(AiGeneratedArticle generatedArticle, AiArticleGenerateRequest request,
                                Long currentUserId, ResearchBrief researchBrief) {
        ArticleCreateRequest articleRequest = new ArticleCreateRequest();
        articleRequest.setTitle(requiredText(generatedArticle.getTitle(), "AI 返回标题为空，请重试"));
        articleRequest.setSummary(generatedArticle.getSummary());
        articleRequest.setContent(requiredText(generatedArticle.getContent(), "AI 返回正文为空，请重试"));
        articleRequest.setType(request.getType());
        articleRequest.setLanguage(request.getLanguage());
        articleRequest.setTags(join(generatedArticle.getTags()));
        articleRequest.setKeywords(join(generatedArticle.getKeywords()));
        articleRequest.setProductConfigId(request.getProductConfigId());
        ArticleDetailVO article = articleService.createArticle(articleRequest, currentUserId);

        int order = 1;
        LocalDateTime now = LocalDateTime.now();
        for (ResearchSourceDraft draft : researchBrief.getSources()) {
            ArticleResearchSource source = new ArticleResearchSource();
            source.setArticleId(article.getId());
            source.setSourceType(draft.getSourceType());
            source.setTitle(draft.getTitle());
            source.setUrl(draft.getUrl());
            source.setDomain(draft.getDomain());
            source.setPublishedAt(draft.getPublishedAt());
            source.setExcerpt(draft.getExcerpt());
            source.setSortOrder(order++);
            source.setRetrievedAt(researchBrief.getRetrievedAt());
            source.setCreatedAt(now);
            sourceMapper.insert(source);
        }
        return article;
    }

    private String requiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value;
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(",", values);
    }
}
