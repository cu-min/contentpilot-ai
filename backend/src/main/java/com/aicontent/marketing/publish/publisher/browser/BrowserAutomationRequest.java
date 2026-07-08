package com.aicontent.marketing.publish.publisher.browser;

public record BrowserAutomationRequest(
        String sessionKey,
        String browserUserDataDir,
        String editorUrl,
        boolean headless,
        double timeoutMs
) {

    private static final double DEFAULT_TIMEOUT_MS = 30_000;

    public double timeoutMsOrDefault() {
        return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }
}
