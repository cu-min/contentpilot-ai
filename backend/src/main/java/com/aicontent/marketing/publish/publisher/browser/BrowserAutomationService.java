package com.aicontent.marketing.publish.publisher.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BrowserAutomationService {

    private static final double DEFAULT_TIMEOUT_MS = 30_000;
    private static final int DEFAULT_VIEWPORT_WIDTH = 1440;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 1000;

    private final Map<String, BrowserAutomationSession> sessions = new ConcurrentHashMap<>();

    public BrowserAutomationSession openSession(BrowserAutomationRequest request) {
        validateRequest(request);
        BrowserAutomationSession existing = sessions.get(request.sessionKey());
        if (existing != null && existing.active()) {
            if (navigate(existing.page(), request.editorUrl(), request.timeoutMsOrDefault())) {
                return existing;
            }
            closeQuietly(existing);
            sessions.remove(request.sessionKey());
        } else {
            closeQuietly(existing);
            sessions.remove(request.sessionKey());
        }

        Playwright playwright = Playwright.create();
        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(request.headless())
                .setViewportSize(DEFAULT_VIEWPORT_WIDTH, DEFAULT_VIEWPORT_HEIGHT);
        BrowserContext context = playwright.chromium().launchPersistentContext(
                Path.of(request.browserUserDataDir()),
                options
        );
        context.setDefaultTimeout(request.timeoutMsOrDefault());
        Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        if (!navigate(page, request.editorUrl(), request.timeoutMsOrDefault())) {
            page = context.newPage();
            navigate(page, request.editorUrl(), request.timeoutMsOrDefault());
        }

        BrowserAutomationSession session = new BrowserAutomationSession(request.sessionKey(), playwright, context, page);
        sessions.put(request.sessionKey(), session);
        return session;
    }

    public String currentUrl(Page page) {
        try {
            return page == null || page.isClosed() ? "" : page.url();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public boolean looksLoggedOut(Page page) {
        String url = currentUrl(page);
        if (containsAny(url, List.of("passport.csdn.net/login", "login", "signin"))) {
            return true;
        }
        return firstVisible(page, List.of(
                "input[type='password']",
                "button:has-text('登录')",
                "text=登录后",
                "text=请登录"
        ), 800);
    }

    public boolean waitForAnyVisibleInPageOrFrames(Page page, List<String> selectors, double timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(500, (long) timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            if (anyVisibleInPageOrFrames(page, selectors)) {
                return true;
            }
            sleep(200);
        }
        return false;
    }

    public boolean anyVisibleInPageOrFrames(Page page, List<String> selectors) {
        if (page == null) {
            return false;
        }
        for (String selector : selectors) {
            if (isVisible(page, selector)) {
                return true;
            }
        }
        try {
            for (Frame frame : page.frames()) {
                for (String selector : selectors) {
                    if (isVisible(frame, selector)) {
                        return true;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    public boolean looksCaptchaBlocked(Page page) {
        return firstVisible(page, List.of(
                "text=验证码",
                "text=安全验证",
                "text=滑块",
                "text=拖动滑块",
                "text=人机验证",
                "text=真人验证",
                "text=风险验证",
                "iframe[src*='captcha']",
                "iframe[src*='verify']",
                "[class*='captcha']",
                "[id*='captcha']",
                "[class*='verify']",
                "[id*='verify']",
                "[class*='slider']",
                "[id*='slider']"
        ), 800);
    }

    public boolean containsTextInPageOrFrames(Page page, String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String snippet = value.length() > 60 ? value.substring(0, 60) : value;
        if (containsText(page, snippet)) {
            return true;
        }
        try {
            for (Frame frame : page.frames()) {
                if (containsText(frame, snippet)) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    public boolean selectorsContainTextOrValue(Page page, List<String> selectors, String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String snippet = value.length() > 60 ? value.substring(0, 60) : value;
        for (String selector : selectors) {
            try {
                if (locatorContains(page.locator(selector).first(), snippet)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Try the next selector.
            }
        }
        try {
            for (Frame frame : page.frames()) {
                for (String selector : selectors) {
                    try {
                        if (locatorContains(frame.locator(selector).first(), snippet)) {
                            return true;
                        }
                    } catch (RuntimeException ignored) {
                        // Try the next selector/frame.
                    }
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    public boolean fillFirst(Page page, List<String> selectors, String value, double timeoutMs) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        for (String selector : selectors) {
            if (tryFill(page, selector, value, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    public boolean fillFirstInPageOrFrames(Page page, List<String> selectors, String value, double timeoutMs) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        if (fillFirst(page, selectors, value, timeoutMs)) {
            return true;
        }
        for (Frame frame : page.frames()) {
            if (tryFillFirst(frame, selectors, value, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    public boolean clickAndTypeFirst(Page page, List<String> selectors, String value, double timeoutMs) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        for (String selector : selectors) {
            if (tryClickAndType(page, selector, value, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    public boolean clickAndInsertFirstInPageOrFrames(Page page, List<String> selectors, String value, double timeoutMs) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        if (clickAndTypeFirst(page, selectors, value, timeoutMs)) {
            return true;
        }
        for (Frame frame : page.frames()) {
            if (tryClickAndInsert(frame, selectors, value, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    public boolean pasteFirstInPageOrFrames(Page page, List<String> selectors, String value, double timeoutMs) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        if (tryPaste(page, selectors, value, timeoutMs)) {
            return true;
        }
        for (Frame frame : page.frames()) {
            if (tryPaste(frame, selectors, value, timeoutMs)) {
                return true;
            }
        }
        return false;
    }

    public boolean fillTagLikeInputs(Page page, List<String> values, List<String> selectors, double timeoutMs) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (String selector : selectors) {
            if (tryFillTagLike(page, selector, values, timeoutMs)) {
                return true;
            }
            for (Frame frame : page.frames()) {
                if (tryFillTagLike(frame, selector, values, timeoutMs)) {
                    return true;
                }
            }
        }
        return false;
    }

    @PreDestroy
    public void closeAll() {
        sessions.values().forEach(this::closeQuietly);
        sessions.clear();
    }

    private void validateRequest(BrowserAutomationRequest request) {
        if (!StringUtils.hasText(request.sessionKey())) {
            throw new IllegalArgumentException("sessionKey is required");
        }
        if (!StringUtils.hasText(request.browserUserDataDir())) {
            throw new IllegalArgumentException("browserUserDataDir is required");
        }
        if (!StringUtils.hasText(request.editorUrl())) {
            throw new IllegalArgumentException("editorUrl is required");
        }
    }

    private boolean navigate(Page page, String editorUrl, double timeoutMs) {
        try {
            if (page == null || page.isClosed()) {
                return false;
            }
            page.navigate(editorUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(timeoutMs));
            return true;
        } catch (PlaywrightException exception) {
            if (isRecoverableNavigationException(exception) && StringUtils.hasText(currentUrl(page))) {
                return true;
            }
            return false;
        } catch (RuntimeException ignored) {
            return StringUtils.hasText(currentUrl(page));
        }
    }

    private boolean tryFill(Page page, String selector, String value, double timeoutMs) {
        try {
            page.locator(selector).first().fill(value, new Locator.FillOptions().setTimeout(timeoutMs));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean tryFillFirst(Frame frame, List<String> selectors, String value, double timeoutMs) {
        for (String selector : selectors) {
            try {
                frame.locator(selector).first().fill(value, new Locator.FillOptions().setTimeout(timeoutMs));
                return true;
            } catch (RuntimeException ignored) {
                // Try the next selector/frame.
            }
        }
        return false;
    }

    private boolean tryClickAndType(Page page, String selector, String value, double timeoutMs) {
        try {
            page.locator(selector).first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
            page.keyboard().press(selectAllShortcut());
            page.keyboard().insertText(value);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean tryClickAndInsert(Frame frame, List<String> selectors, String value, double timeoutMs) {
        for (String selector : selectors) {
            try {
                frame.locator(selector).first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
                frame.page().keyboard().press(selectAllShortcut());
                frame.page().keyboard().insertText(value);
                return true;
            } catch (RuntimeException ignored) {
                // Try the next selector/frame.
            }
        }
        return false;
    }

    private boolean tryPaste(Page page, List<String> selectors, String value, double timeoutMs) {
        for (String selector : selectors) {
            try {
                page.locator(selector).first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
                writeClipboard(page, value);
                page.keyboard().press(pasteShortcut());
                return true;
            } catch (RuntimeException ignored) {
                // Clipboard access may be unavailable; try the next selector.
            }
        }
        return false;
    }

    private boolean tryPaste(Frame frame, List<String> selectors, String value, double timeoutMs) {
        for (String selector : selectors) {
            try {
                frame.locator(selector).first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
                writeClipboard(frame.page(), value);
                frame.page().keyboard().press(pasteShortcut());
                return true;
            } catch (RuntimeException ignored) {
                // Clipboard access may be unavailable; try the next selector/frame.
            }
        }
        return false;
    }

    private boolean tryFillTagLike(Page page, String selector, List<String> values, double timeoutMs) {
        try {
            page.locator(selector).first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
            insertTagValues(page, values);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean tryFillTagLike(Frame frame, String selector, List<String> values, double timeoutMs) {
        try {
            frame.locator(selector).first().click(new Locator.ClickOptions().setTimeout(timeoutMs));
            insertTagValues(frame.page(), values);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void insertTagValues(Page page, List<String> values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                page.keyboard().insertText(value);
                page.keyboard().press("Enter");
            }
        }
    }

    private void writeClipboard(Page page, String value) {
        page.evaluate("text => navigator.clipboard.writeText(text)", value);
    }

    private boolean firstVisible(Page page, List<String> selectors, double timeoutMs) {
        for (String selector : selectors) {
            try {
                page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
                return true;
            } catch (TimeoutError ignored) {
                // Keep probing lightweight login/captcha hints.
            } catch (RuntimeException ignored) {
                // Some text selectors may be invalid for a specific page state.
            }
        }
        return false;
    }

    private boolean containsAny(String value, List<String> needles) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String lowerValue = value.toLowerCase();
        return needles.stream().anyMatch(lowerValue::contains);
    }

    private boolean isVisible(Page page, String selector) {
        try {
            return page.locator(selector).first().isVisible();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isVisible(Frame frame, String selector) {
        try {
            return frame.locator(selector).first().isVisible();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean containsText(Page page, String value) {
        try {
            String bodyText = page.locator("body").textContent();
            return StringUtils.hasText(bodyText) && bodyText.contains(value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean containsText(Frame frame, String value) {
        try {
            String bodyText = frame.locator("body").textContent();
            return StringUtils.hasText(bodyText) && bodyText.contains(value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean locatorContains(Locator locator, String value) {
        try {
            String inputValue = locator.inputValue();
            if (StringUtils.hasText(inputValue) && inputValue.contains(value)) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // Non-input locators do not support inputValue.
        }
        try {
            String textContent = locator.textContent();
            return StringUtils.hasText(textContent) && textContent.contains(value);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isRecoverableNavigationException(RuntimeException exception) {
        String message = exception.getMessage();
        return StringUtils.hasText(message)
                && (message.contains("Object doesn't exist: response@")
                || message.contains("Object doesn't exist: request@")
                || message.contains("net::ERR_ABORTED"));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String selectAllShortcut() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac") ? "Meta+A" : "Control+A";
    }

    private String pasteShortcut() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac") ? "Meta+V" : "Control+V";
    }

    private void closeQuietly(BrowserAutomationSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (RuntimeException ignored) {
            // Best-effort cleanup only.
        }
    }
}
