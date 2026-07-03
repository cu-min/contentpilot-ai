package com.aicontent.marketing.platformcontent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_platform_content")
public class ArticlePlatformContent {

    @TableId(type = IdType.AUTO)
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
}
