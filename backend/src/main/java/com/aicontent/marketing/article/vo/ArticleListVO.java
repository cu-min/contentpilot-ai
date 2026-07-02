package com.aicontent.marketing.article.vo;

import com.aicontent.marketing.article.entity.Article;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleListVO {

    private Long id;

    private String title;

    private String summary;

    private String type;

    private String language;

    private String status;

    private String tags;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static ArticleListVO from(Article article) {
        ArticleListVO vo = new ArticleListVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setType(article.getType());
        vo.setLanguage(article.getLanguage());
        vo.setStatus(article.getStatus());
        vo.setTags(article.getTags());
        vo.setCreatedBy(article.getCreatedBy());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }
}
