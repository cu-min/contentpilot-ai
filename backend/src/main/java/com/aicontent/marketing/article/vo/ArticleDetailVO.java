package com.aicontent.marketing.article.vo;

import com.aicontent.marketing.article.entity.Article;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArticleDetailVO {

    private Long id;

    private String title;

    private String summary;

    private String content;

    private String type;

    private String language;

    private String status;

    private String tags;

    private String keywords;

    private Long productConfigId;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ArticleResearchSourceVO> researchSources = new ArrayList<>();

    public static ArticleDetailVO from(Article article) {
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        vo.setType(article.getType());
        vo.setLanguage(article.getLanguage());
        vo.setStatus(article.getStatus());
        vo.setTags(article.getTags());
        vo.setKeywords(article.getKeywords());
        vo.setProductConfigId(article.getProductConfigId());
        vo.setCreatedBy(article.getCreatedBy());
        vo.setUpdatedBy(article.getUpdatedBy());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }
}
