package com.aicontent.marketing.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_research_source")
public class ArticleResearchSource {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String sourceType;
    private String title;
    private String url;
    private String domain;
    private LocalDateTime publishedAt;
    private String excerpt;
    private Integer sortOrder;
    private LocalDateTime retrievedAt;
    private LocalDateTime createdAt;
}
