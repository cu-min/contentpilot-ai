package com.aicontent.marketing.ai.service;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.research.ResearchBrief;
import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.article.vo.ArticleDetailVO;

public interface AiGeneratedArticlePersistenceService {

    ArticleDetailVO save(AiGeneratedArticle generatedArticle, AiArticleGenerateRequest request,
                         Long currentUserId, ResearchBrief researchBrief);
}
