package com.aicontent.marketing.publish.publisher;

public record PublishResult(
        String type,
        String taskStatus,
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
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_NEED_LOGIN = "NEED_LOGIN";
    private static final String STATUS_NEED_CAPTCHA = "NEED_CAPTCHA";
    private static final String STATUS_WAITING_MANUAL_CONFIRM = "WAITING_MANUAL_CONFIRM";
    private static final String STATUS_LINK_FETCH_FAILED = "LINK_FETCH_FAILED";
    private static final String STATUS_CONTENT_REJECTED = "CONTENT_REJECTED";

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
        return new PublishResult(TYPE_SUCCESS, STATUS_SUCCESS, true, publishUrl, draftId, null, draftUrl, articleId, message, null);
    }

    public static PublishResult submitted(
            String publishUrl,
            String draftId,
            String publishId,
            String draftUrl,
            String message
    ) {
        return new PublishResult(TYPE_SUBMITTED, STATUS_RUNNING, false, publishUrl, draftId, publishId, draftUrl, null, message, null);
    }

    public static PublishResult prepared(String draftId, String draftUrl, String message) {
        return new PublishResult(
                STATUS_WAITING_MANUAL_CONFIRM,
                STATUS_WAITING_MANUAL_CONFIRM,
                false,
                null,
                draftId,
                null,
                draftUrl,
                null,
                message,
                null
        );
    }

    public static PublishResult failed(String errorMessage) {
        return failed(errorMessage, null, null, null);
    }

    public static PublishResult failed(String errorMessage, String draftId, String draftUrl, String articleId) {
        return new PublishResult(TYPE_FAILED, STATUS_FAILED, false, null, draftId, null, draftUrl, articleId, null, errorMessage);
    }

    public static PublishResult needLogin(String publishUrl, String message) {
        return blocked(STATUS_NEED_LOGIN, publishUrl, null, message);
    }

    public static PublishResult needCaptcha(String publishUrl, String message) {
        return blocked(STATUS_NEED_CAPTCHA, publishUrl, null, message);
    }

    public static PublishResult needManualConfirm(String publishUrl, String draftUrl, String message) {
        return prepared(null, firstText(draftUrl, publishUrl), message);
    }

    public static PublishResult linkFetchFailed(String publishUrl, String message) {
        return blocked(STATUS_LINK_FETCH_FAILED, publishUrl, null, message);
    }

    public static PublishResult contentRejected(String publishUrl, String message) {
        return blocked(STATUS_CONTENT_REJECTED, publishUrl, null, message);
    }

    private static PublishResult blocked(String taskStatus, String publishUrl, String draftUrl, String message) {
        return new PublishResult(taskStatus, taskStatus, false, publishUrl, null, null, draftUrl, null, message, message);
    }

    public boolean submitted() {
        return TYPE_SUBMITTED.equals(type);
    }

    public boolean blocked() {
        return !TYPE_SUCCESS.equals(type) && !TYPE_FAILED.equals(type) && !TYPE_SUBMITTED.equals(type);
    }

    public boolean prepared() {
        return STATUS_WAITING_MANUAL_CONFIRM.equals(taskStatus);
    }

    private static String firstText(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
