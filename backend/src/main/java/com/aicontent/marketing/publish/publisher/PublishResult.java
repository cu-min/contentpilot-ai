package com.aicontent.marketing.publish.publisher;

public record PublishResult(
        String type,
        boolean success,
        String publishUrl,
        String draftId,
        String publishId,
        String draftUrl,
        String articleId,
        String message,
        String errorMessage
) {

    private static final String TYPE_SUCCESS = "SUCCESS";
    private static final String TYPE_FAILED = "FAILED";
    private static final String TYPE_SUBMITTED = "SUBMITTED";

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
        return new PublishResult(TYPE_SUCCESS, true, publishUrl, draftId, null, draftUrl, articleId, message, null);
    }

    public static PublishResult submitted(
            String publishUrl,
            String draftId,
            String publishId,
            String draftUrl,
            String message
    ) {
        return new PublishResult(TYPE_SUBMITTED, false, publishUrl, draftId, publishId, draftUrl, null, message, null);
    }

    public static PublishResult failed(String errorMessage) {
        return failed(errorMessage, null, null, null);
    }

    public static PublishResult failed(String errorMessage, String draftId, String draftUrl, String articleId) {
        return new PublishResult(TYPE_FAILED, false, null, draftId, null, draftUrl, articleId, null, errorMessage);
    }

    public boolean submitted() {
        return TYPE_SUBMITTED.equals(type);
    }
}
