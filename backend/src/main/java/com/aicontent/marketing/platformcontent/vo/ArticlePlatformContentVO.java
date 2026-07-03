package com.aicontent.marketing.platformcontent.vo;

import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticlePlatformContentVO {

    private Long id;

    private Long articleId;

    private String platform;

    private String title;

    private String summary;

    private String content;

    private String tags;

    private String keywords;

    private String status;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static ArticlePlatformContentVO from(ArticlePlatformContent content) {
        ArticlePlatformContentVO vo = new ArticlePlatformContentVO();
        vo.setId(content.getId());
        vo.setArticleId(content.getArticleId());
        vo.setPlatform(content.getPlatform());
        vo.setTitle(content.getTitle());
        vo.setSummary(content.getSummary());
        vo.setContent(content.getContent());
        vo.setTags(content.getTags());
        vo.setKeywords(content.getKeywords());
        vo.setStatus(content.getStatus());
        vo.setCreatedBy(content.getCreatedBy());
        vo.setUpdatedBy(content.getUpdatedBy());
        vo.setCreatedAt(content.getCreatedAt());
        vo.setUpdatedAt(content.getUpdatedAt());
        return vo;
    }
}
