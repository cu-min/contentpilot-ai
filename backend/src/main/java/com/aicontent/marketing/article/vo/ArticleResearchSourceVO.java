package com.aicontent.marketing.article.vo;

import com.aicontent.marketing.article.entity.ArticleResearchSource;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleResearchSourceVO {

    private String sourceType;
    private String title;
    private String url;
    private String domain;
    private LocalDateTime publishedAt;
    private String excerpt;
    private Integer sortOrder;
    private LocalDateTime retrievedAt;

    public static ArticleResearchSourceVO from(ArticleResearchSource source) {
        ArticleResearchSourceVO vo = new ArticleResearchSourceVO();
        vo.setSourceType(source.getSourceType());
        vo.setTitle(source.getTitle());
        vo.setUrl(source.getUrl());
        vo.setDomain(source.getDomain());
        vo.setPublishedAt(source.getPublishedAt());
        vo.setExcerpt(source.getExcerpt());
        vo.setSortOrder(source.getSortOrder());
        vo.setRetrievedAt(source.getRetrievedAt());
        return vo;
    }
}
