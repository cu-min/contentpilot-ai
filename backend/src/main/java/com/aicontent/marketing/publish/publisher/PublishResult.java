package com.aicontent.marketing.publish.publisher;

public record PublishResult(
        boolean success,
        String publishUrl,
        String draftId,
        String draftUrl,
        String articleId,
        String message,
        String errorMessage
) {

    public static PublishResult success(String publishUrl, String message) {
        return success(publishUrl, null, null, null, message);
    }

    public static PublishResult success(
            String publishUrl,
            String draftId,
            String draftUrl,
            String articleId,
            String message
    ) {
        return new PublishResult(true, publishUrl, draftId, draftUrl, articleId, message, null);
    }

    public static PublishResult failed(String errorMessage) {
        return failed(errorMessage, null, null, null);
    }

    public static PublishResult failed(String errorMessage, String draftId, String draftUrl, String articleId) {
        return new PublishResult(false, null, draftId, draftUrl, articleId, null, errorMessage);
    }
}
