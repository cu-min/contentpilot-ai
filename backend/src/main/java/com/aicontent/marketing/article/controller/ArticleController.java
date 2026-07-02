package com.aicontent.marketing.article.controller;

import com.aicontent.marketing.article.dto.ArticleCreateRequest;
import com.aicontent.marketing.article.dto.ArticleQueryRequest;
import com.aicontent.marketing.article.dto.ArticleUpdateRequest;
import com.aicontent.marketing.article.service.ArticleService;
import com.aicontent.marketing.article.vo.ArticleDetailVO;
import com.aicontent.marketing.article.vo.ArticleListVO;
import com.aicontent.marketing.auth.security.LoginUser;
import com.aicontent.marketing.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Result<Page<ArticleListVO>> list(ArticleQueryRequest request) {
        return Result.success(articleService.listArticles(request));
    }

    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleService.getArticleDetail(id));
    }

    @PostMapping
    public Result<ArticleDetailVO> create(
            @Valid @RequestBody ArticleCreateRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(articleService.createArticle(request, loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<ArticleDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleUpdateRequest request,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        return Result.success(articleService.updateArticle(id, request, loginUser.getUserId()));
    }

    @PutMapping("/{id}/archive")
    public Result<Void> archive(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        articleService.archiveArticle(id, loginUser.getUserId());
        return Result.success();
    }

    @PutMapping("/{id}/restore")
    public Result<Void> restore(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser
    ) {
        articleService.restoreArticle(id, loginUser.getUserId());
        return Result.success();
    }
}
