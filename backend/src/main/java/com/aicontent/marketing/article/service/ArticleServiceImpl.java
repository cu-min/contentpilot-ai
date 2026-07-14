package com.aicontent.marketing.article.service;

import com.aicontent.marketing.article.dto.ArticleCreateRequest;
import com.aicontent.marketing.article.dto.ArticleQueryRequest;
import com.aicontent.marketing.article.dto.ArticleUpdateRequest;
import com.aicontent.marketing.article.entity.Article;
import com.aicontent.marketing.article.mapper.ArticleMapper;
import com.aicontent.marketing.article.mapper.ArticleResearchSourceMapper;
import com.aicontent.marketing.article.vo.ArticleDetailVO;
import com.aicontent.marketing.article.vo.ArticleListVO;
import com.aicontent.marketing.article.vo.ArticleResearchSourceVO;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.common.result.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final Set<String> ARTICLE_TYPES = Set.of(
            "PRODUCT_INTRO",
            "TUTORIAL",
            "INDUSTRY_KNOWLEDGE",
            "COMPARISON",
            "SOLUTION",
            "SEO"
    );

    private static final Set<String> LANGUAGES = Set.of("ZH", "EN");

    private final ArticleResearchSourceMapper sourceMapper;

    public ArticleServiceImpl(ArticleResearchSourceMapper sourceMapper) {
        this.sourceMapper = sourceMapper;
    }

    private static final Set<String> STATUSES = Set.of(
            "DRAFT",
            "PENDING",
            "PUBLISHED",
            "FAILED",
            "ARCHIVED"
    );

    @Override
    public Page<ArticleListVO> listArticles(ArticleQueryRequest request) {
        validateOptionalFilters(request);
        Page<Article> articlePage = new Page<>(normalizePage(request.getPage()), normalizeSize(request.getSize()));
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .last("ORDER BY CASE WHEN status = 'ARCHIVED' THEN 1 ELSE 0 END ASC, updated_at DESC, id DESC");

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(query -> query
                    .like(Article::getTitle, request.getKeyword())
                    .or()
                    .like(Article::getSummary, request.getKeyword())
                    .or()
                    .like(Article::getTags, request.getKeyword())
                    .or()
                    .like(Article::getKeywords, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Article::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getType())) {
            wrapper.eq(Article::getType, request.getType());
        }
        if (StringUtils.hasText(request.getLanguage())) {
            wrapper.eq(Article::getLanguage, request.getLanguage());
        }

        Page<Article> result = page(articlePage, wrapper);
        List<ArticleListVO> records = result.getRecords().stream().map(ArticleListVO::from).toList();
        Page<ArticleListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        ArticleDetailVO detail = ArticleDetailVO.from(getRequiredArticle(id));
        detail.setResearchSources(sourceMapper.selectList(new LambdaQueryWrapper<com.aicontent.marketing.article.entity.ArticleResearchSource>()
                        .eq(com.aicontent.marketing.article.entity.ArticleResearchSource::getArticleId, id)
                        .orderByAsc(com.aicontent.marketing.article.entity.ArticleResearchSource::getSortOrder))
                .stream().map(ArticleResearchSourceVO::from).toList());
        return detail;
    }

    @Override
    public ArticleDetailVO createArticle(ArticleCreateRequest request, Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setType(request.getType());
        article.setLanguage(request.getLanguage());
        article.setStatus(STATUS_DRAFT);
        article.setTags(request.getTags());
        article.setKeywords(request.getKeywords());
        article.setProductConfigId(request.getProductConfigId());
        article.setCreatedBy(currentUserId);
        article.setUpdatedBy(currentUserId);
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        save(article);
        return ArticleDetailVO.from(article);
    }

    @Override
    public ArticleDetailVO updateArticle(Long id, ArticleUpdateRequest request, Long currentUserId) {
        Article article = getRequiredArticle(id);
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setType(request.getType());
        article.setLanguage(request.getLanguage());
        article.setTags(request.getTags());
        article.setKeywords(request.getKeywords());
        article.setProductConfigId(request.getProductConfigId());
        article.setUpdatedBy(currentUserId);
        article.setUpdatedAt(LocalDateTime.now());
        updateById(article);
        return ArticleDetailVO.from(article);
    }

    @Override
    public void archiveArticle(Long id, Long currentUserId) {
        Article article = getRequiredArticle(id);
        article.setStatus(STATUS_ARCHIVED);
        article.setUpdatedBy(currentUserId);
        article.setUpdatedAt(LocalDateTime.now());
        updateById(article);
    }

    @Override
    public void restoreArticle(Long id, Long currentUserId) {
        Article article = getRequiredArticle(id);
        if (STATUS_ARCHIVED.equals(article.getStatus())) {
            article.setStatus(STATUS_DRAFT);
            article.setUpdatedBy(currentUserId);
            article.setUpdatedAt(LocalDateTime.now());
            updateById(article);
        }
    }

    private Article getRequiredArticle(Long id) {
        Article article = getById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "article not found");
        }
        return article;
    }

    private void validateOptionalFilters(ArticleQueryRequest request) {
        if (StringUtils.hasText(request.getStatus()) && !STATUSES.contains(request.getStatus())) {
            throw new BusinessException("status is invalid");
        }
        if (StringUtils.hasText(request.getType()) && !ARTICLE_TYPES.contains(request.getType())) {
            throw new BusinessException("type is invalid");
        }
        if (StringUtils.hasText(request.getLanguage()) && !LANGUAGES.contains(request.getLanguage())) {
            throw new BusinessException("language is invalid");
        }
    }

    private long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
