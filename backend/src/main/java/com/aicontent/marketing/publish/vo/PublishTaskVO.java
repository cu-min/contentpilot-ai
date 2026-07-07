package com.aicontent.marketing.publish.vo;

import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.publish.entity.PublishTask;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublishTaskVO {

    private Long id;

    private Long articleId;

    private Long platformContentId;

    private String platformContentTitle;

    private String platform;

    private Long accountId;

    private String accountName;

    private String title;

    private String status;

    private String publishType;

    private LocalDateTime scheduleTime;

    private String publishMode;

    private String publishUrl;

    private String externalDraftId;

    private String externalPublishId;

    private String externalArticleId;

    private String draftUrl;

    private String articleStatus;

    private String errorMessage;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static PublishTaskVO from(PublishTask task, ArticlePlatformContent content, PlatformAccount account) {
        PublishTaskVO vo = new PublishTaskVO();
        vo.setId(task.getId());
        vo.setArticleId(task.getArticleId());
        vo.setPlatformContentId(task.getPlatformContentId());
        vo.setPlatformContentTitle(content == null ? "" : content.getTitle());
        vo.setPlatform(task.getPlatform());
        vo.setAccountId(task.getAccountId());
        vo.setAccountName(account == null ? "" : account.getAccountName());
        vo.setTitle(task.getTitle());
        vo.setStatus(task.getStatus());
        vo.setPublishType(task.getPublishType());
        vo.setScheduleTime(task.getScheduleTime());
        vo.setPublishMode(task.getPublishMode());
        vo.setPublishUrl(task.getPublishUrl());
        vo.setExternalDraftId(task.getExternalDraftId());
        vo.setExternalPublishId(task.getExternalPublishId());
        vo.setExternalArticleId(task.getExternalArticleId());
        vo.setDraftUrl(task.getDraftUrl());
        vo.setArticleStatus(task.getArticleStatus());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setUpdatedBy(task.getUpdatedBy());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }
}
