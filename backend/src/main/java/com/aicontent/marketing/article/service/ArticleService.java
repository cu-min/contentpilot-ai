package com.aicontent.marketing.article.service;

import com.aicontent.marketing.article.dto.ArticleCreateRequest;
import com.aicontent.marketing.article.dto.ArticleQueryRequest;
import com.aicontent.marketing.article.dto.ArticleUpdateRequest;
import com.aicontent.marketing.article.entity.Article;
import com.aicontent.marketing.article.vo.ArticleDetailVO;
import com.aicontent.marketing.article.vo.ArticleListVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ArticleService extends IService<Article> {

    Page<ArticleListVO> listArticles(ArticleQueryRequest request);

    ArticleDetailVO getArticleDetail(Long id);

    ArticleDetailVO createArticle(ArticleCreateRequest request, Long currentUserId);

    ArticleDetailVO updateArticle(Long id, ArticleUpdateRequest request, Long currentUserId);

    void archiveArticle(Long id, Long currentUserId);

    void restoreArticle(Long id, Long currentUserId);
}
