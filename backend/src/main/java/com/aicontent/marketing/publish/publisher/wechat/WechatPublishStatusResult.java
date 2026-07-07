package com.aicontent.marketing.publish.publisher.wechat;

public record WechatPublishStatusResult(
        String state,
        Integer publishStatus,
        String articleId,
        String articleUrl,
        String errorMessage,
        String rawSummary
) {

    public static WechatPublishStatusResult processing(Integer publishStatus, String rawSummary) {
        return new WechatPublishStatusResult("processing", publishStatus, null, null, null, rawSummary);
    }

    public static WechatPublishStatusResult success(
            Integer publishStatus,
            String articleId,
            String articleUrl,
            String rawSummary
    ) {
        return new WechatPublishStatusResult("success", publishStatus, articleId, articleUrl, null, rawSummary);
    }

    public static WechatPublishStatusResult failed(
            Integer publishStatus,
            String errorMessage,
            String rawSummary
    ) {
        return new WechatPublishStatusResult("failed", publishStatus, null, null, errorMessage, rawSummary);
    }

    public boolean processing() {
        return "processing".equals(state);
    }

    public boolean success() {
        return "success".equals(state);
    }

    public boolean failed() {
        return "failed".equals(state);
    }
}
