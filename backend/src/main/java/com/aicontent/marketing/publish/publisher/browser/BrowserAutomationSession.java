package com.aicontent.marketing.publish.publisher.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public record BrowserAutomationSession(
        String sessionKey,
        Playwright playwright,
        BrowserContext context,
        Page page
) implements AutoCloseable {

    public boolean active() {
        return page != null && !page.isClosed();
    }

    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
