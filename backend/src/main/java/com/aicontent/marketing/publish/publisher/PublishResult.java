package com.aicontent.marketing.publish.publisher;

public record PublishResult(
        boolean success,
        String publishUrl,
        String message,
        String errorMessage
) {

    public static PublishResult success(String publishUrl, String message) {
        return new PublishResult(true, publishUrl, message, null);
    }

    public static PublishResult failed(String errorMessage) {
        return new PublishResult(false, null, null, errorMessage);
    }
}
