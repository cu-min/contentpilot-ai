package com.aicontent.marketing.ai.controller;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.ai.service.AiArticleService;
import com.aicontent.marketing.ai.vo.AiArticleGenerateVO;
import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/articles")
public class AiArticleController {

    private final AiArticleService aiArticleService;

    public AiArticleController(AiArticleService aiArticleService) {
        this.aiArticleService = aiArticleService;
    }

    @PostMapping("/generate")
    public Result<AiArticleGenerateVO> generate(
            @Valid @RequestBody AiArticleGenerateRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(aiArticleService.generateArticle(request, loginUser.getUserId()));
    }
}
