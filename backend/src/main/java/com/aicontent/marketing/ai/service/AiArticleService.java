package com.aicontent.marketing.ai.service;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.vo.AiArticleGenerateSubmitVO;
import com.aicontent.marketing.ai.vo.AiGenerationTaskVO;

public interface AiArticleService {

    AiArticleGenerateSubmitVO generateArticle(AiArticleGenerateRequest request, Long currentUserId);

    AiGenerationTaskVO getGenerationTask(Long taskId, Long currentUserId);
}
