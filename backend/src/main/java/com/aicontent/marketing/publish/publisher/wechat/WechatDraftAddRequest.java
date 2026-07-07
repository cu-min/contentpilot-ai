package com.aicontent.marketing.publish.publisher.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WechatDraftAddRequest(List<Article> articles) {

    public record Article(
            String title,
            String author,
            String digest,
            String content,
            @JsonProperty("thumb_media_id") String thumbMediaId,
            @JsonProperty("content_source_url") String contentSourceUrl,
            @JsonProperty("need_open_comment") int needOpenComment,
            @JsonProperty("only_fans_can_comment") int onlyFansCanComment
    ) {
    }
}
