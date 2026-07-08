package com.aicontent.marketing.publish.publisher.browser;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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
            navigate(existing.page(), request.editorUrl(), request.timeoutMsOrDefault());
            return existing;
        }
        closeQuietly(existing);

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
        navigate(page, request.editorUrl(), request.timeoutMsOrDefault());

        BrowserAutomationSession session = new BrowserAutomationSession(request.sessionKey(), playwright, context, page);
        sessions.put(request.sessionKey(), session);
        return session;
    }

    public boolean looksLoggedOut(Page page) {
        String url = page.url();
        if (containsAny(url, List.of("login", "signin", "passport"))) {
            return true;
        }
        return firstVisible(page, List.of(
                "input[type='password']",
                "button:has-text('登录')",
                "text=登录后",
                "text=请登录"
        ), 800);
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

    private void navigate(Page page, String editorUrl, double timeoutMs) {
        page.navigate(editorUrl, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                .setTimeout(timeoutMs));
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
