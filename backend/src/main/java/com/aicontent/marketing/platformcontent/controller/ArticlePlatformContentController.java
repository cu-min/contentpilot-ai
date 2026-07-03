package com.aicontent.marketing.platformcontent.controller;

import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.platformcontent.dto.PlatformContentGenerateRequest;
import com.aicontent.marketing.platformcontent.dto.PlatformContentUpdateRequest;
import com.aicontent.marketing.platformcontent.service.ArticlePlatformContentService;
import com.aicontent.marketing.platformcontent.vo.ArticlePlatformContentVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ArticlePlatformContentController {

    private final ArticlePlatformContentService platformContentService;

    public ArticlePlatformContentController(ArticlePlatformContentService platformContentService) {
        this.platformContentService = platformContentService;
    }

    @PostMapping("/articles/{articleId}/platform-contents/generate")
    public Result<List<ArticlePlatformContentVO>> generate(
            @PathVariable Long articleId,
            @Valid @RequestBody PlatformContentGenerateRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(platformContentService.generate(articleId, request, loginUser.getUserId()));
    }

    @GetMapping("/articles/{articleId}/platform-contents")
    public Result<List<ArticlePlatformContentVO>> list(@PathVariable Long articleId) {
        return Result.success(platformContentService.listByArticleId(articleId));
    }

    @GetMapping("/platform-contents/{id}")
    public Result<ArticlePlatformContentVO> detail(@PathVariable Long id) {
        return Result.success(platformContentService.getDetail(id));
    }

    @PutMapping("/platform-contents/{id}")
    public Result<ArticlePlatformContentVO> update(
            @PathVariable Long id,
            @Valid @RequestBody PlatformContentUpdateRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(platformContentService.updateContent(id, request, loginUser.getUserId()));
    }

    @PutMapping("/platform-contents/{id}/archive")
    public Result<Void> archive(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        platformContentService.archive(id, loginUser.getUserId());
        return Result.success();
    }

    @PutMapping("/platform-contents/{id}/restore")
    public Result<Void> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        platformContentService.restore(id, loginUser.getUserId());
        return Result.success();
    }
}
