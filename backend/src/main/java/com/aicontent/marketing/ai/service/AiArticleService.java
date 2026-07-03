package com.aicontent.marketing.ai.service;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.vo.AiArticleGenerateVO;

public interface AiArticleService {

    AiArticleGenerateVO generateArticle(AiArticleGenerateRequest request, Long currentUserId);
}
