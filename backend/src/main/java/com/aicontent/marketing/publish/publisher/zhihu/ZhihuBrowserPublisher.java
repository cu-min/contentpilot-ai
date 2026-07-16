package com.aicontent.marketing.publish.publisher.zhihu;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.publisher.browser.BrowserAutomationRequest;
import com.aicontent.marketing.publish.publisher.browser.BrowserAutomationService;
import com.aicontent.marketing.publish.publisher.browser.BrowserAutomationSession;
import com.aicontent.marketing.publish.publisher.browser.BrowserPublisherConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ZhihuBrowserPublisher implements PlatformPublisher {

    private static final String PLATFORM = "ZHIHU";
    private static final String MODE_BROWSER_AUTOMATION = "BROWSER_AUTOMATION";
    private static final String DEFAULT_EDITOR_URL = "https://zhuanlan.zhihu.com/write";
    private static final String DEFAULT_MANAGE_URL = "https://www.zhihu.com/creator";
    private static final String MANUAL_CONFIRM_MESSAGE = "已自动填充知乎编辑器，请切换到 Chrome for Testing 窗口检查并手动发布";
    private static final Logger log = LoggerFactory.getLogger(ZhihuBrowserPublisher.class);
    private static final long TITLE_TOTAL_TIMEOUT_MS = 15_000;
    private static final double TITLE_STRATEGY_TIMEOUT_MS = 2_500;
    private static final long TITLE_STRATEGY_BUDGET_MS = 3_000;
    private static final long CONTENT_TOTAL_TIMEOUT_MS = 20_000;
    private static final double CONTENT_STRATEGY_TIMEOUT_MS = 3_000;
    private static final long CONTENT_STRATEGY_BUDGET_MS = 4_000;
    private static final long PUBLISH_SETTINGS_TIMEOUT_MS = 8_000;
    private static final long PUBLISH_RESULT_TIMEOUT_MS = 45_000;
    private static final int CONTENT_VERIFY_SNAPSHOT_LENGTH = 12_000;
    private static final Pattern VERIFY_TEXT_FRAGMENT_PATTERN = Pattern.compile("[\\p{IsHan}A-Za-z0-9]{6,}");
    private static final List<String> TITLE_INPUT_SELECTORS = List.of(
            "input[placeholder*='标题']",
            "textarea[placeholder*='标题']"
    );
    private static final List<String> TITLE_SELECTORS = List.of(
            "input[placeholder*='标题']",
            "textarea[placeholder*='标题']",
            "[contenteditable='true'][placeholder*='标题']",
            "[contenteditable='true'][aria-label*='标题']",
            "[role='textbox'][aria-label*='标题']",
            "text=请输入标题"
    );
    private static final List<String> CONTENT_EDITABLE_SELECTORS = List.of(
            "[contenteditable='true']:not([placeholder*='标题']):not([aria-label*='标题'])",
            ".ProseMirror",
            ".DraftEditor-editorContainer [contenteditable='true']",
            "[data-slate-editor='true']",
            "[role='textbox']:not([aria-label*='标题'])"
    );
    private static final List<String> CONTENT_FALLBACK_SELECTORS = List.of(
            "textarea:not([placeholder*='标题'])",
            "textarea[placeholder*='正文']",
            "textarea[placeholder*='内容']",
            "textarea[placeholder*='写文章']"
    );
    private static final List<String> EDITOR_READY_SELECTORS = List.of(
            "input[placeholder*='标题']",
            "textarea[placeholder*='标题']",
            "[contenteditable='true'][placeholder*='标题']",
            "[contenteditable='true'][aria-label*='标题']",
            "text=请输入标题",
            ".ProseMirror",
            ".DraftEditor-editorContainer",
            "[data-slate-editor='true']",
            "[contenteditable='true']",
            "button:has-text('发布')",
            "button:has-text('发布文章')"
    );
    private static final List<String> PUBLISH_BUTTON_SELECTORS = List.of(
            "button:has-text('发布文章')",
            "button:has-text('发布')",
            "text=发布文章",
            "text=发布"
    );

    private final BrowserAutomationService browserAutomationService;
    private final ObjectMapper objectMapper;
    private final ReentrantLock automationLock = new ReentrantLock();

    public ZhihuBrowserPublisher(BrowserAutomationService browserAutomationService, ObjectMapper objectMapper) {
        this.browserAutomationService = browserAutomationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public String mode() {
        return MODE_BROWSER_AUTOMATION;
    }

    @Override
    public PublishResult publish(PublishContext context) {
        boolean locked = false;
        try {
            locked = automationLock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("Zhihu browser automation rejected: another task is running, taskId={}", context.taskId());
                return PublishResult.failed("已有知乎自动化任务正在执行，请完成后再重试。");
            }
            validateContext(context);
            BrowserPublisherConfig config = BrowserPublisherConfig.parse(
                    context.accountAuthConfig(),
                    objectMapper,
                    DEFAULT_EDITOR_URL,
                    DEFAULT_MANAGE_URL
            );
            log.info("Zhihu editor open started: taskId={}, editorUrl={}", context.taskId(), config.editorUrl());
            BrowserAutomationSession session = browserAutomationService.openSession(new BrowserAutomationRequest(
                    sessionKey(context),
                    config.browserUserDataDir(),
                    config.editorUrl(),
                    config.headless(),
                    config.timeoutMs()
            ));
            Page page = selectEditorPage(session, config);

            LoginDetection loginDetection = detectZhihuLoginRequired(page);
            if (loginDetection.required()) {
                return needLoginResult(page, config, loginDetection.reason(), "知乎登录状态失效，请在打开的浏览器中完成登录后重新执行发布任务");
            }
            CaptchaDetection captchaDetection = detectZhihuCaptchaRequired(page);
            if (captchaDetection.required()) {
                return needCaptchaResult(page, config, captchaDetection.reason());
            }
            if (!waitForEditorReady(page, config.timeoutMs())) {
                loginDetection = detectZhihuLoginRequired(page);
                if (loginDetection.required()) {
                    return needLoginResult(page, config, loginDetection.reason(), "知乎登录状态失效，请在打开的浏览器中完成登录后重新执行发布任务");
                }
                captchaDetection = detectZhihuCaptchaRequired(page);
                if (captchaDetection.required()) {
                    return needCaptchaResult(page, config, captchaDetection.reason());
                }
                return PublishResult.failed("知乎编辑器未就绪，未检测到标题或正文可编辑区域");
            }

            FillOutcome titleOutcome = fillTitle(page, context.title());
            if (!titleOutcome.success()) {
                return PublishResult.failed("知乎标题填充失败，未能在页面中校验到目标标题");
            }
            ContentFillOutcome contentOutcome = fillContent(page, context.content(), config);
            if (!contentOutcome.success()) {
                if (contentOutcome.weakSuccess()) {
                    log.warn("Zhihu content fill weakly accepted for manual confirm: taskId={}, strategy={}",
                            context.taskId(), contentOutcome.strategy());
                } else {
                    return PublishResult.failed("知乎正文填充失败，未能在页面中校验到正文内容");
                }
            }
            handlePostFillPrompts(page);
            PublishResult result = prepareForManualConfirm(context, page, config);
            log.info("Zhihu publish preparation result status = {}", result.taskStatus());
            return result;
        } catch (BusinessException exception) {
            return PublishResult.failed(exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return PublishResult.failed("知乎自动化任务等待执行锁被中断");
        } catch (RuntimeException exception) {
            return PublishResult.linkFetchFailed(null, "知乎编辑器页面打开或填充失败：" + safeMessage(exception));
        } finally {
            if (locked) {
                automationLock.unlock();
            }
        }
    }

    private Page selectEditorPage(BrowserAutomationSession session, BrowserPublisherConfig config) {
        long deadline = System.currentTimeMillis() + Math.max(5_000, (long) config.timeoutMs());
        Page selected = session.page();
        while (System.currentTimeMillis() < deadline) {
            log.info("Zhihu editor ready check");
            Page best = bestEditorPage(session);
            if (best != null) {
                bringToFront(best);
                log.info("Zhihu editor page selected: url={}", safeUrl(best));
                return best;
            }
            sleep(500);
        }
        Page fallback = bestZhihuPage(session);
        if (fallback != null) {
            bringToFront(fallback);
            log.info("Zhihu editor page selected: url={}", safeUrl(fallback));
            return fallback;
        }
        log.info("Zhihu editor page selected: url={}", safeUrl(selected));
        return selected;
    }

    private Page bestEditorPage(BrowserAutomationSession session) {
        List<Page> pages = browserAutomationService.activePages(session);
        for (int index = pages.size() - 1; index >= 0; index--) {
            Page page = pages.get(index);
            if (isZhihuPage(page) && hasEditorDom(page)) {
                return page;
            }
        }
        return null;
    }

    private Page bestZhihuPage(BrowserAutomationSession session) {
        List<Page> pages = browserAutomationService.activePages(session);
        for (int index = pages.size() - 1; index >= 0; index--) {
            Page page = pages.get(index);
            if (isZhihuPage(page)) {
                return page;
            }
        }
        return null;
    }

    private boolean waitForEditorReady(Page page, double timeoutMs) {
        log.info("Zhihu editor ready check");
        if (hasEditorDom(page)) {
            return true;
        }
        return browserAutomationService.waitForAnyVisibleInPageOrFrames(page, EDITOR_READY_SELECTORS, Math.min(timeoutMs, 15_000));
    }

    private boolean hasEditorDom(Page page) {
        try {
            Object ready = page.evaluate("""
                    () => {
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                      };
                      const title = Array.from(document.querySelectorAll('input, textarea, [contenteditable="true"], [role="textbox"]'))
                        .some(element => visible(element) && /标题|请输入标题|title/i.test([
                          element.getAttribute('placeholder') || '',
                          element.getAttribute('aria-label') || '',
                          element.innerText || '',
                          element.textContent || ''
                        ].join(' ')));
                      const editor = Array.from(document.querySelectorAll('[contenteditable="true"], .ProseMirror, .DraftEditor-editorContainer, [data-slate-editor="true"], textarea'))
                        .some(element => visible(element));
                      return Boolean(title && editor);
                    }
                    """);
            return Boolean.TRUE.equals(ready);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private LoginDetection detectZhihuLoginRequired(Page page) {
        String url = browserAutomationService.currentUrl(page).toLowerCase();
        if (StringUtils.hasText(url) && (url.contains("signin") || url.contains("login"))) {
            return new LoginDetection(true, "url-login");
        }
        String reason = visibleLoginReason(page);
        return StringUtils.hasText(reason)
                ? new LoginDetection(true, reason)
                : new LoginDetection(false, "");
    }

    private String visibleLoginReason(Page page) {
        try {
            Object reason = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const expiredPhrases = ['请登录', '登录已过期', '请重新登录', '登录态失效'];
                      const roots = Array.from(document.querySelectorAll([
                        '[role="dialog"]',
                        '[role="alert"]',
                        '.Modal',
                        '.modal',
                        '[class*="Modal"]',
                        '[class*="modal"]',
                        '[class*="Dialog"]',
                        '[class*="dialog"]',
                        '[class*="Toast"]',
                        '[class*="toast"]',
                        '[class*="Message"]',
                        '[class*="message"]',
                        '[class*="Login"]',
                        '[class*="login"]'
                      ].join(','))).filter(visible);
                      const expired = roots.find(root => {
                        const snapshot = text(root.innerText || root.textContent || '');
                        return snapshot && snapshot.length < 1200 && expiredPhrases.some(phrase => snapshot.includes(phrase));
                      });
                      if (expired) return 'expired-text-visible';
                      const loginModal = roots.find(root => {
                        const snapshot = text(root.innerText || root.textContent || '');
                        if (!snapshot || snapshot.length > 1200) return false;
                        const hasLoginCopy = ['密码登录', '验证码登录', '短信登录', '登录知乎', '登录/注册', '注册/登录']
                          .some(phrase => snapshot.includes(phrase));
                        const hasLoginAction = Array.from(root.querySelectorAll('button, a')).some(element => {
                          const label = text(element.innerText || element.textContent || element.getAttribute('aria-label') || '');
                          return visible(element) && (label === '登录' || label === '登录/注册' || label === '注册/登录');
                        });
                        return hasLoginCopy && hasLoginAction;
                      });
                      if (loginModal) return 'login-modal-visible';
                      return '';
                    }
                    """);
            return reason instanceof String text ? text : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private CaptchaDetection detectZhihuCaptchaRequired(Page page) {
        try {
            Object visible = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const phrases = ['验证码', '安全验证', '滑块', '人机验证', '异常流量'];
                      const roots = Array.from(document.querySelectorAll('body, [role="dialog"], [role="alert"], [class*="captcha"], [class*="Captcha"], [class*="verify"], [class*="Verify"], [class*="slider"], [class*="Slider"]'))
                        .filter(visible);
                      const textMatched = roots.some(root => {
                        const snapshot = text(root.innerText || root.textContent || '');
                        return snapshot.length < 5000 && phrases.some(phrase => snapshot.includes(phrase));
                      });
                      const widgetMatched = Array.from(document.querySelectorAll('iframe[src*="captcha"], iframe[src*="verify"], [class*="captcha"], [id*="captcha"], [class*="verify"], [id*="verify"], [class*="slider"], [id*="slider"]'))
                        .some(visible);
                      return Boolean(textMatched || widgetMatched);
                    }
                    """);
            return Boolean.TRUE.equals(visible)
                    ? new CaptchaDetection(true, "captcha-visible")
                    : new CaptchaDetection(false, "");
        } catch (RuntimeException ignored) {
            return new CaptchaDetection(false, "");
        }
    }

    private PublishResult needLoginResult(Page page, BrowserPublisherConfig config, String reason, String message) {
        log.warn("Zhihu login required detected: reason={}, currentUrl={}", reason, safeUrl(page));
        return PublishResult.needLogin(currentOrConfiguredUrl(page, config), message);
    }

    private PublishResult needCaptchaResult(Page page, BrowserPublisherConfig config, String reason) {
        log.warn("Zhihu captcha required detected: reason={}, currentUrl={}", reason, safeUrl(page));
        return PublishResult.needCaptcha(currentOrConfiguredUrl(page, config), "知乎需要验证码或安全验证，请人工处理后重新执行");
    }

    private FillOutcome fillTitle(Page page, String title) {
        String normalizedTitle = normalize(title, "未命名知乎文章");
        long startedAt = System.currentTimeMillis();
        long deadlineMs = startedAt + TITLE_TOTAL_TIMEOUT_MS;
        log.info("Zhihu title fill started: url={}, titleLength={}", safeUrl(page), normalizedTitle.length());
        if (runTitleStrategy(page, normalizedTitle, "direct-input-fill", startedAt, deadlineMs,
                () -> fillVisibleTitleInput(page, normalizedTitle, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS)))) {
            return titleSuccess("direct-input-fill", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "click-active-insert", startedAt, deadlineMs,
                () -> clickTitleAndInsert(page, normalizedTitle, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS)))) {
            return titleSuccess("click-active-insert", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "clipboard-paste", startedAt, deadlineMs,
                () -> pasteTitle(page, normalizedTitle, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS)))) {
            return titleSuccess("clipboard-paste", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "js-setter", startedAt, deadlineMs,
                () -> setTitleByJs(page, normalizedTitle))) {
            return titleSuccess("js-setter", startedAt);
        }
        return titleFailed("all-strategies", startedAt, elapsed(startedAt) >= TITLE_TOTAL_TIMEOUT_MS);
    }

    private boolean runTitleStrategy(Page page, String title, String strategy, long startedAt, long deadlineMs, BooleanSupplier action) {
        if (System.currentTimeMillis() >= deadlineMs) {
            log.warn("Zhihu title fill strategy failed: strategy={}, reason=timeout, durationMs={}", strategy, elapsed(startedAt));
            return false;
        }
        log.info("Zhihu title fill strategy started: strategy={}, durationMs={}", strategy, elapsed(startedAt));
        boolean attempted;
        try {
            attempted = action.getAsBoolean();
        } catch (RuntimeException exception) {
            log.warn("Zhihu title fill strategy failed: strategy={}, reason={}, durationMs={}",
                    strategy, safeMessage(exception), elapsed(startedAt));
            return false;
        }
        if (attempted && titleFilled(page, title)) {
            log.info("Zhihu title fill strategy succeeded: strategy={}, durationMs={}", strategy, elapsed(startedAt));
            return true;
        }
        log.warn("Zhihu title fill strategy failed: strategy={}, attempted={}, durationMs={}", strategy, attempted, elapsed(startedAt));
        return false;
    }

    private boolean fillVisibleTitleInput(Page page, String title, long deadlineMs) {
        for (String selector : TITLE_INPUT_SELECTORS) {
            if (System.currentTimeMillis() >= deadlineMs) {
                return false;
            }
            try {
                Locator locator = page.locator(selector).first();
                locator.fill(title, new Locator.FillOptions().setTimeout(boundedTimeout(deadlineMs, TITLE_STRATEGY_TIMEOUT_MS)));
                return true;
            } catch (RuntimeException ignored) {
                // Try the next title input.
            }
        }
        return false;
    }

    private boolean clickTitleAndInsert(Page page, String title, long deadlineMs) {
        if (!clickFirst(page, TITLE_SELECTORS, deadlineMs)) {
            return false;
        }
        page.keyboard().press(selectAllShortcut());
        page.keyboard().insertText(title);
        return true;
    }

    private boolean pasteTitle(Page page, String title, long deadlineMs) {
        if (!clickFirst(page, TITLE_SELECTORS, deadlineMs)) {
            return false;
        }
        writeClipboard(page, title);
        page.keyboard().press(pasteShortcut());
        return true;
    }

    private boolean setTitleByJs(Page page, String title) {
        Object filled = page.evaluate("""
                title => {
                  const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = window.getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                  };
                  const textOf = element => [
                    element.getAttribute?.('placeholder') || '',
                    element.getAttribute?.('aria-label') || '',
                    element.getAttribute?.('title') || '',
                    element.getAttribute?.('name') || '',
                    element.id || '',
                    element.innerText || '',
                    element.textContent || ''
                  ].join(' ');
                  const looksTitle = element => /标题|请输入标题|title/i.test(textOf(element));
                  const setNativeValue = (element, value) => {
                    if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
                      const prototype = element instanceof HTMLInputElement ? HTMLInputElement.prototype : HTMLTextAreaElement.prototype;
                      const descriptor = Object.getOwnPropertyDescriptor(prototype, 'value');
                      element.focus();
                      descriptor?.set?.call(element, value);
                      element.dispatchEvent(new Event('input', { bubbles: true }));
                      element.dispatchEvent(new Event('change', { bubbles: true }));
                      return true;
                    }
                    if (element.isContentEditable || element.getAttribute('contenteditable') === 'true') {
                      element.focus();
                      element.textContent = value;
                      element.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: value }));
                      element.dispatchEvent(new Event('change', { bubbles: true }));
                      return true;
                    }
                    return false;
                  };
                  const selectors = [
                    "input[placeholder*='标题']",
                    "textarea[placeholder*='标题']",
                    "[contenteditable='true'][placeholder*='标题']",
                    "[contenteditable='true'][aria-label*='标题']",
                    "[role='textbox'][aria-label*='标题']"
                  ];
                  for (const selector of selectors) {
                    for (const element of document.querySelectorAll(selector)) {
                      if (visible(element) && looksTitle(element) && setNativeValue(element, title)) return true;
                    }
                  }
                  const textNodes = Array.from(document.querySelectorAll('div, p, span')).filter(element => visible(element) && /请输入标题/.test(textOf(element)));
                  const placeholder = textNodes[0];
                  if (placeholder) {
                    placeholder.click();
                    const active = document.activeElement;
                    if (active && setNativeValue(active, title)) return true;
                  }
                  return false;
                }
                """, title);
        return Boolean.TRUE.equals(filled);
    }

    private ContentFillOutcome fillContent(Page page, String content, BrowserPublisherConfig config) {
        String normalizedContent = normalize(content, "");
        long startedAt = System.currentTimeMillis();
        long deadlineMs = startedAt + CONTENT_TOTAL_TIMEOUT_MS;
        String contentPreview = preview(normalizedContent);
        if (!StringUtils.hasText(normalizedContent)) {
            log.warn("Zhihu content fill failed: reason=empty-content");
            return new ContentFillOutcome(false, false, "empty-content");
        }
        if (attemptContentPaste(page, normalizedContent, "paste-contenteditable", startedAt, config,
                () -> browserAutomationService.pasteFirstInPageOrFramesWithin(page, CONTENT_EDITABLE_SELECTORS, normalizedContent,
                        CONTENT_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, CONTENT_STRATEGY_BUDGET_MS)))) {
            return contentSuccess("paste-contenteditable", startedAt);
        }
        if (attemptContentPaste(page, normalizedContent, "paste-textarea", startedAt, config,
                () -> browserAutomationService.pasteFirstInPageOrFramesWithin(page, CONTENT_FALLBACK_SELECTORS, normalizedContent,
                        CONTENT_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, CONTENT_STRATEGY_BUDGET_MS)))) {
            return contentSuccess("paste-textarea", startedAt);
        }
        if (attemptContentPaste(page, normalizedContent, "coordinate-paste", startedAt, config,
                () -> pasteByEditorCoordinate(page, normalizedContent))) {
            return contentSuccess("coordinate-paste", startedAt);
        }
        log.warn("Zhihu content fill failed: contentLength={}, contentPreview={}, durationMs={}",
                normalizedContent.length(), contentPreview, elapsed(startedAt));
        boolean weakSuccess = isZhihuEditorPage(page) && StringUtils.hasText(normalizedContent) && elapsed(startedAt) < CONTENT_TOTAL_TIMEOUT_MS;
        return new ContentFillOutcome(false, weakSuccess, "all-strategies");
    }

    private boolean attemptContentPaste(
            Page page,
            String content,
            String strategy,
            long startedAt,
            BrowserPublisherConfig config,
            BooleanSupplier action
    ) {
        String contentPreview = preview(content);
        log.info("Zhihu content paste attempted: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, content.length(), contentPreview, elapsed(startedAt));
        boolean attempted;
        try {
            attempted = action.getAsBoolean();
        } catch (RuntimeException exception) {
            log.warn("Zhihu content paste finished: strategy={}, success=false, reason={}, durationMs={}",
                    strategy, safeMessage(exception), elapsed(startedAt));
            return false;
        }
        log.info("Zhihu content paste finished: strategy={}, success={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, attempted, content.length(), contentPreview, elapsed(startedAt));
        if (!attempted) {
            return false;
        }
        sleep(Math.max(0, (long) config.waitAfterFillMs()));
        log.info("Zhihu content verify started: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, content.length(), contentPreview, elapsed(startedAt));
        logContentSnapshot(page);
        boolean verified = contentFilled(page, content);
        if (verified) {
            log.info("Zhihu content verify succeeded: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                    strategy, content.length(), contentPreview, elapsed(startedAt));
            return true;
        }
        log.warn("Zhihu content verify failed: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, content.length(), contentPreview, elapsed(startedAt));
        return false;
    }

    private boolean pasteByEditorCoordinate(Page page, String content) {
        try {
            @SuppressWarnings("unchecked")
            List<Number> point = (List<Number>) page.evaluate("""
                    () => {
                      const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                      };
                      const candidates = Array.from(document.querySelectorAll('[contenteditable="true"], .ProseMirror, .DraftEditor-editorContainer, [data-slate-editor="true"], textarea'))
                        .filter(visible)
                        .filter(element => !/标题|title/i.test([
                          element.getAttribute('placeholder') || '',
                          element.getAttribute('aria-label') || ''
                        ].join(' ')));
                      const element = candidates[0];
                      if (!element) return [];
                      const rect = element.getBoundingClientRect();
                      return [Math.max(20, rect.left + Math.min(120, rect.width / 2)), Math.max(20, rect.top + Math.min(80, rect.height / 2))];
                    }
                    """);
            if (point == null || point.size() < 2) {
                return false;
            }
            return browserAutomationService.clickAtAndPaste(page, point.get(0).doubleValue(), point.get(1).doubleValue(), content);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private PublishResult manualConfirmResult(PublishContext context, Page page, BrowserPublisherConfig config) {
        String url = currentOrConfiguredUrl(page, config);
        log.info("Zhihu manual confirm result returned: taskId={}, url={}", context.taskId(), url);
        return PublishResult.needManualConfirm(url, url, MANUAL_CONFIRM_MESSAGE);
    }

    private void handlePostFillPrompts(Page page) {
        confirmDraftLoadingModal(page);
        confirmMarkdownParsePrompt(page);
    }

    private void confirmDraftLoadingModal(Page page) {
        try {
            Object clicked = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const dialogs = Array.from(document.querySelectorAll('[role="dialog"], .Modal, .modal, [class*="Modal"], [class*="modal"], [class*="Dialog"], [class*="dialog"]'))
                        .filter(visible)
                        .filter(root => text(root.innerText || root.textContent || '').includes('草稿加载中'));
                      const dialog = dialogs[0];
                      if (!dialog) return false;
                      const button = Array.from(dialog.querySelectorAll('button, a'))
                        .filter(visible)
                        .find(element => text(element.innerText || element.textContent || element.getAttribute('aria-label') || '') === '确定');
                      if (!button) return false;
                      button.click();
                      return true;
                    }
                    """);
            if (Boolean.TRUE.equals(clicked)) {
                log.info("Zhihu draft loading modal confirmed");
                sleep(1_000);
            }
        } catch (RuntimeException exception) {
            log.warn("Zhihu draft loading modal confirm failed: reason={}", safeMessage(exception));
        }
    }

    private void confirmMarkdownParsePrompt(Page page) {
        try {
            Object clicked = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const roots = Array.from(document.querySelectorAll('body, [role="alert"], [class*="toast"], [class*="Toast"], [class*="message"], [class*="Message"], div'))
                        .filter(visible)
                        .map(element => ({ element, snapshot: text(element.innerText || element.textContent || '') }))
                        .filter(item => item.snapshot.includes('识别到特殊格式') && item.snapshot.includes('Markdown'))
                        .sort((a, b) => a.snapshot.length - b.snapshot.length);
                      const root = (roots[0] || {}).element;
                      if (!root) return false;
                      const button = Array.from(root.querySelectorAll('button, a, span'))
                        .filter(visible)
                        .find(element => text(element.innerText || element.textContent || element.getAttribute('aria-label') || '') === '确认并解析');
                      if (!button) return false;
                      button.click();
                      return true;
                    }
                    """);
            if (Boolean.TRUE.equals(clicked)) {
                log.info("Zhihu markdown parse prompt confirmed");
                sleep(1_000);
            }
        } catch (RuntimeException exception) {
            log.warn("Zhihu markdown parse prompt confirm failed: reason={}", safeMessage(exception));
        }
    }

    private PublishResult prepareForManualConfirm(PublishContext context, Page page, BrowserPublisherConfig config) {
        long startedAt = System.currentTimeMillis();
        LoginDetection loginDetection = detectZhihuLoginRequired(page);
        if (loginDetection.required()) {
            return needLoginResult(page, config, loginDetection.reason(), "知乎登录状态失效，请在打开的浏览器中完成登录后重新执行发布任务");
        }
        CaptchaDetection captchaDetection = detectZhihuCaptchaRequired(page);
        if (captchaDetection.required()) {
            return needCaptchaResult(page, config, captchaDetection.reason());
        }
        if (!isZhihuEditorPage(page)) {
            log.warn("Zhihu publish result failed reason=not-editor currentUrl={}", safeUrl(page));
            return PublishResult.failed("知乎发布失败：当前页面不是知乎写文章编辑器");
        }
        if (!titleFilled(page, context.title()) || !contentFilled(page, context.content())) {
            log.warn("Zhihu publish result failed reason=content-not-ready currentUrl={}", safeUrl(page));
            return PublishResult.failed("知乎发布失败：标题或正文尚未填充完成");
        }
        scrollToBottom(page);
        if (!waitForPublishSettings(page)) {
            log.warn("Zhihu publish result failed reason=publish-settings-not-found currentUrl={}", safeUrl(page));
            return PublishResult.failed("知乎发布失败：未检测到发布设置区域");
        }
        handleTopicAndColumn(page, context, config);
        if (!publishButtonVisible(page)) {
            return PublishResult.failed("知乎发布准备失败：未找到人工发布按钮");
        }
        log.info("Zhihu publish settings prepared without clicking publish: taskId={}, durationMs={}",
                context.taskId(), elapsed(startedAt));
        return manualConfirmResult(context, page, config);
    }

    private boolean publishButtonVisible(Page page) {
        return browserAutomationService.anyVisibleInPageOrFrames(page, PUBLISH_BUTTON_SELECTORS);
    }

    private boolean clickPublishButton(Page page) {
        scrollToBottom(page);
        if (clickBottomPublishButtonByJs(page)) {
            log.info("Zhihu publish button found");
            log.info("Zhihu publish button clicked");
            return true;
        }
        if (clickPublishButtonByJs(page)) {
            log.info("Zhihu publish button found");
            log.info("Zhihu publish button clicked");
            return true;
        }
        return false;
    }

    private boolean clickBottomPublishButtonByJs(Page page) {
        try {
            Object clicked = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const settings = findPublishSettingsRoot();
                      const settingsRect = settings?.getBoundingClientRect?.();
                      const buttons = Array.from(document.querySelectorAll('button, a'))
                        .filter(visible)
                        .map(element => ({ element, label: text(element.innerText || element.textContent || element.getAttribute('aria-label') || '') }))
                        .filter(item => item.label === '发布')
                        .filter(item => !/预览|草稿|取消|关闭/.test(item.label));
                      const candidates = buttons.filter(item => {
                        const rect = item.element.getBoundingClientRect();
                        return settings
                          ? (settings.contains(item.element) || rect.top >= settingsRect.top - 80)
                          : rect.top > window.innerHeight * 0.45;
                      });
                      const button = (candidates[candidates.length - 1] || buttons[buttons.length - 1] || {}).element;
                      if (!button) return false;
                      button.scrollIntoView({ block: 'center', inline: 'center' });
                      button.click();
                      return true;

                      function findPublishSettingsRoot() {
                        const candidates = Array.from(document.querySelectorAll('section, form, div, main, article'))
                          .filter(visible)
                          .map(element => ({ element, snapshot: text(element.innerText || element.textContent || '') }))
                          .filter(item => item.snapshot.includes('发布设置'))
                          .sort((a, b) => a.snapshot.length - b.snapshot.length);
                        return (candidates[0] || {}).element || null;
                      }
                    }
                    """);
            return Boolean.TRUE.equals(clicked);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean clickPublishButtonByJs(Page page) {
        try {
            Object clicked = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const roots = [];
                      const dialogs = Array.from(document.querySelectorAll('[role="dialog"], .Modal, .modal, [class*="Modal"], [class*="modal"], [class*="Dialog"], [class*="dialog"]')).filter(visible);
                      roots.push(document.body);
                      for (const root of roots) {
                        const buttons = Array.from(root.querySelectorAll('button, a'))
                          .filter(visible)
                          .filter(element => {
                            const label = text(element.innerText || element.textContent || element.getAttribute('aria-label') || '');
                            if (!['发布', '发布文章', '确认发布', '继续发布'].includes(label)) return false;
                            return !/草稿|预览|取消|关闭/.test(label);
                          });
                        const button = buttons[buttons.length - 1];
                        if (button) {
                          button.scrollIntoView({ block: 'center', inline: 'center' });
                          button.click();
                          return true;
                        }
                      }
                      return false;
                    }
                    """);
            return Boolean.TRUE.equals(clicked);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean finalPublishButtonVisible(Page page) {
        try {
            Object found = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const dialogs = Array.from(document.querySelectorAll('[role="dialog"], .Modal, .modal, [class*="Modal"], [class*="modal"], [class*="Dialog"], [class*="dialog"]'))
                        .filter(visible);
                      return dialogs.some(root => Array.from(root.querySelectorAll('button, a'))
                        .filter(visible)
                        .some(element => ['发布', '发布文章', '确认发布', '继续发布'].includes(
                          text(element.innerText || element.textContent || element.getAttribute('aria-label') || '')
                        )));
                    }
                    """);
            return Boolean.TRUE.equals(found);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean waitForPublishSettings(Page page) {
        long deadline = System.currentTimeMillis() + PUBLISH_SETTINGS_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            handlePostFillPrompts(page);
            if (publishSettingsDetected(page)) {
                log.info("Zhihu publish settings detected");
                return true;
            }
            scrollToBottom(page);
            sleep(500);
        }
        return false;
    }

    private boolean publishSettingsDetected(Page page) {
        try {
            Object detected = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const snapshot = text(document.body?.innerText || document.body?.textContent || '');
                      const hasSettings = snapshot.includes('发布设置');
                      const hasAnyField = ['添加封面', '投稿至问题', '创作声明', '文章话题', '内容来源'].some(label => snapshot.includes(label));
                      const hasPublishButton = Array.from(document.querySelectorAll('button, a'))
                        .filter(visible)
                        .some(element => text(element.innerText || element.textContent || element.getAttribute('aria-label') || '') === '发布');
                      return Boolean(hasSettings && hasAnyField && hasPublishButton);
                    }
                    """);
            return Boolean.TRUE.equals(detected);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void handleTopicAndColumn(Page page, PublishContext context, BrowserPublisherConfig config) {
        tryFillTopic(page, context, config);
        trySelectColumn(page, config);
    }

    private void tryFillTopic(Page page, PublishContext context, BrowserPublisherConfig config) {
        int existingCount = existingTopicCount(page);
        log.info("Zhihu topic existing detected: count={}", existingCount);
        if (existingCount > 0) {
            return;
        }
        List<String> topics = resolveTopicCandidates(context, config);
        if (topics.isEmpty() || !topicPromptVisible(page)) {
            log.warn("Zhihu topic fill failed: reason=no-topic-candidate-or-prompt");
            return;
        }
        log.info("Zhihu topic fill started: count={}", topics.size());
        try {
            boolean filled = browserAutomationService.fillTagLikeInputsWithin(page, topics, List.of(
                    "input[placeholder*='话题']",
                    "input[aria-label*='话题']",
                    "[role='combobox'][aria-label*='话题']",
                    "[contenteditable='true'][aria-label*='话题']"
            ), 1_000, System.currentTimeMillis() + 3_000);
            if (!filled) {
                browserAutomationService.clickTextInPageOrFrames(page, List.of("+ 添加话题", "添加话题", "文章话题", "选择话题"), 800);
                for (String topic : topics) {
                    page.keyboard().insertText(topic);
                    page.keyboard().press("Enter");
                    sleep(300);
                }
            }
            sleep(500);
            int countAfterFill = existingTopicCount(page);
            if (filled || countAfterFill > 0) {
                log.info("Zhihu topic fill succeeded: count={}", countAfterFill);
            } else {
                log.warn("Zhihu topic fill failed: reason=not-verified");
            }
        } catch (RuntimeException exception) {
            log.warn("Zhihu topic fill failed: reason={}", safeMessage(exception));
            // Topic is optional unless Zhihu explicitly reports it as required.
        }
    }

    private int existingTopicCount(Page page) {
        try {
            Object count = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      const labels = Array.from(document.querySelectorAll('label, span, div, p'))
                        .filter(visible)
                        .filter(element => text(element.innerText || element.textContent) === '文章话题');
                      const label = labels[0];
                      if (!label) return 0;
                      const row = closestRow(label);
                      if (!row) return 0;
                      const topicTexts = new Set();
                      const elements = Array.from(row.querySelectorAll('button, span, div, a'))
                        .filter(visible)
                        .map(element => text(element.innerText || element.textContent || element.getAttribute('aria-label') || ''))
                        .filter(value => value && !['文章话题', '+ 添加话题', '添加话题', '×', 'x'].includes(value))
                        .filter(value => !value.includes('添加话题') && value.length <= 30);
                      for (const value of elements) {
                        const cleaned = value.replace(/[×x]$/i, '').trim();
                        if (cleaned) topicTexts.add(cleaned);
                      }
                      return topicTexts.size;

                      function closestRow(element) {
                        let current = element;
                        for (let depth = 0; current && depth < 8; depth += 1) {
                          const snapshot = text(current.innerText || current.textContent || '');
                          if (snapshot.includes('文章话题') && (snapshot.includes('添加话题') || current.querySelector('button, input, [contenteditable="true"]'))) {
                            return current;
                          }
                          current = current.parentElement;
                        }
                        return label.parentElement;
                      }
                    }
                    """);
            return count instanceof Number number ? number.intValue() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private List<String> resolveTopicCandidates(PublishContext context, BrowserPublisherConfig config) {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        if (config.defaultTopics() != null) {
            config.defaultTopics().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(topics::add);
        }
        addDelimitedTopics(topics, context.tags());
        addDelimitedTopics(topics, context.keywords());
        return topics.stream().limit(2).toList();
    }

    private void addDelimitedTopics(Set<String> topics, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        for (String item : value.split("[,，、;；\\s]+")) {
            String topic = item.trim();
            if (StringUtils.hasText(topic)) {
                topics.add(topic);
            }
        }
    }

    private void trySelectColumn(Page page, BrowserPublisherConfig config) {
        try {
            if (StringUtils.hasText(config.defaultColumn())) {
                if (browserAutomationService.clickTextInPageOrFrames(page, List.of(config.defaultColumn()), 800)) {
                    return;
                }
                browserAutomationService.fillFirstInPageOrFramesWithin(page, List.of(
                        "input[placeholder*='专栏']",
                        "input[aria-label*='专栏']",
                        "[role='combobox'][aria-label*='专栏']"
                ), config.defaultColumn(), 800, System.currentTimeMillis() + 2_000);
                page.keyboard().press("Enter");
                return;
            }
            browserAutomationService.clickTextInPageOrFrames(page, List.of("不发布到专栏", "不选择专栏"), 800);
        } catch (RuntimeException ignored) {
            // Column is optional unless Zhihu blocks final publish.
        }
    }

    private PublishResult waitForPublishResult(Page page, PublishContext context, BrowserPublisherConfig config, long startedAt) {
        log.info("Zhihu publish result check started: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
        long deadline = System.currentTimeMillis() + PUBLISH_RESULT_TIMEOUT_MS;
        boolean triedManagePage = false;
        while (System.currentTimeMillis() < deadline) {
            handlePostFillPrompts(page);
            LoginDetection loginDetection = detectZhihuLoginRequired(page);
            if (loginDetection.required()) {
                return needLoginResult(page, config, loginDetection.reason(), "知乎登录状态失效，请在打开的浏览器中完成登录后重新执行发布任务");
            }
            CaptchaDetection captchaDetection = detectZhihuCaptchaRequired(page);
            if (captchaDetection.required()) {
                return needCaptchaResult(page, config, captchaDetection.reason());
            }
            if (publishDialogDetected(page)) {
                String validationError = visiblePublishValidationError(page);
                if (StringUtils.hasText(validationError)) {
                    log.warn("Zhihu publish validation error detected: reason={}", validationError);
                    return PublishResult.failed("知乎发布失败：" + validationError);
                }
                if (elapsed(startedAt) > 8_000) {
                    log.warn("Zhihu publish result failed reason=dialog-still-visible durationMs={}", elapsed(startedAt));
                    return PublishResult.failed("知乎发布未完成，发布弹窗仍存在");
                }
                sleep(500);
                continue;
            }
            String currentUrl = safeUrl(page);
            String articleUrl = extractArticleUrl(page, context.title());
            if (isZhihuArticleUrl(currentUrl)) {
                log.info("Zhihu publish result success by url: taskId={}, publishUrl={}", context.taskId(), currentUrl);
                return PublishResult.success(currentUrl, "知乎发布成功");
            }
            if (isZhihuArticleUrl(articleUrl)) {
                log.info("Zhihu publish result success by url: taskId={}, publishUrl={}", context.taskId(), articleUrl);
                return PublishResult.success(articleUrl, "知乎发布成功");
            }
            String snapshot = pageSnapshot(page);
            if (looksContentRejected(snapshot)) {
                log.warn("Zhihu publish result failed reason=content-rejected currentUrl={}", currentUrl);
                return PublishResult.contentRejected(currentUrl, "知乎内容审核失败或触发平台规则，请在知乎创作中心查看详情");
            }
            if (visiblePublishSuccessText(page)) {
                String publishUrl = isZhihuArticleUrl(articleUrl) ? articleUrl : fallbackManageUrl(config);
                String message = isZhihuArticleUrl(publishUrl)
                        ? "知乎发布成功"
                        : "知乎发布成功，但未自动获取正式文章链接，请在创作中心查看";
                log.info("Zhihu publish result success by success text: taskId={}, publishUrl={}", context.taskId(), publishUrl);
                return PublishResult.success(publishUrl, message);
            }
            String validationError = visiblePublishValidationError(page);
            if (StringUtils.hasText(validationError)) {
                log.warn("Zhihu publish validation error detected: reason={}", validationError);
                return PublishResult.failed("知乎发布失败：" + validationError);
            }
            if (!triedManagePage && elapsed(startedAt) > 8_000) {
                triedManagePage = true;
                navigateToManagePage(page, config);
            }
            if (triedManagePage && managePagePublishedTitleMatch(page, context.title())) {
                articleUrl = extractArticleUrl(page, context.title());
                String publishUrl = isZhihuArticleUrl(articleUrl) ? articleUrl : fallbackManageUrl(config);
                String message = isZhihuArticleUrl(publishUrl)
                        ? "知乎发布成功"
                        : "知乎发布成功，但未自动获取正式文章链接，请在创作中心查看";
                log.info("Zhihu publish result success by manage page match: taskId={}, publishUrl={}", context.taskId(), publishUrl);
                return PublishResult.success(publishUrl, message);
            }
            sleep(1_000);
        }
        log.warn("Zhihu publish result failed reason=link-fetch-failed currentUrl={}", safeUrl(page));
        return PublishResult.linkFetchFailed(fallbackManageUrl(config), "知乎发布后未能确认结果，请在知乎创作中心检查");
    }

    private boolean visiblePublishSuccessText(Page page) {
        String snapshot = pageSnapshot(page);
        return containsAny(snapshot, List.of("发布成功", "已发布", "提交成功", "审核中"));
    }

    private boolean looksContentRejected(String snapshot) {
        return containsAny(snapshot, List.of("审核失败", "审核未通过", "内容违规", "违规", "未通过审核", "发布失败"));
    }

    private String visiblePublishValidationError(Page page) {
        String snapshot = pageSnapshot(page);
        if (containsAny(snapshot, List.of("请添加话题", "请填写话题", "请选择话题"))) {
            return "文章话题未填写";
        }
        if (containsAny(snapshot, List.of("内容不能为空", "正文不能为空", "标题不能为空", "请填写正文", "请填写标题"))) {
            return "标题或正文未填写";
        }
        return "";
    }

    private boolean publishDialogDetected(Page page) {
        try {
            Object detected = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && style.opacity !== '0';
                      };
                      return Array.from(document.querySelectorAll('[role="dialog"], .Modal, .modal, [class*="Modal"], [class*="modal"], [class*="Dialog"], [class*="dialog"]'))
                        .filter(visible)
                        .some(root => {
                          const snapshot = text(root.innerText || root.textContent || '');
                          return snapshot.includes('发布') && !snapshot.includes('登录知乎');
                        });
                    }
                    """);
            return Boolean.TRUE.equals(detected);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean topicPromptVisible(Page page) {
        String snapshot = pageSnapshot(page);
        return containsAny(snapshot, List.of("添加话题", "文章话题", "选择话题"));
    }

    private boolean managePagePublishedTitleMatch(Page page, String title) {
        if (!StringUtils.hasText(title)) {
            return false;
        }
        return snapshotContainsContent(pageSnapshot(page), title);
    }

    private String extractArticleUrl(Page page, String title) {
        try {
            Object value = page.evaluate("""
                    title => {
                      const normalize = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const visible = element => {
                        if (!element || element.closest('script, style, template') || element.getAttribute('aria-hidden') === 'true') return false;
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                      };
                      const titleText = normalize(title);
                      const candidates = Array.from(document.querySelectorAll('a[href]'))
                        .filter(visible)
                        .map(link => ({ href: link.href || '', text: normalize(link.innerText || link.textContent || '') }))
                        .filter(item => item.href.includes('zhihu.com') && (item.href.includes('/p/') || item.href.includes('/question/')));
                      const exact = candidates.find(item => titleText && item.text.includes(titleText));
                      return (exact || candidates[0] || {}).href || '';
                    }
                    """, title);
            return value instanceof String text ? text : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private boolean titleFilled(Page page, String title) {
        String snapshot = browserAutomationService.textSnapshot(page, titleSelectorsForSnapshot(), 4_000);
        return snapshotContainsContent(snapshot, title);
    }

    private boolean contentFilled(Page page, String content) {
        String snapshot = browserAutomationService.textSnapshot(page, contentSelectorsForSnapshot(), CONTENT_VERIFY_SNAPSHOT_LENGTH);
        return snapshotContainsContent(snapshot, content);
    }

    private boolean snapshotContainsContent(String snapshot, String content) {
        if (!StringUtils.hasText(snapshot) || !StringUtils.hasText(content)) {
            return false;
        }
        String normalizedSnapshot = normalizeForVerify(snapshot);
        for (String fragment : verifyFragments(content)) {
            String normalizedFragment = normalizeForVerify(fragment);
            if (StringUtils.hasText(normalizedFragment) && normalizedSnapshot.contains(normalizedFragment)) {
                return true;
            }
        }
        return false;
    }

    private List<String> verifyFragments(String content) {
        Set<String> fragments = new LinkedHashSet<>();
        String normalized = normalizeForVerify(content);
        if (normalized.length() >= 20) {
            fragments.add(normalized.substring(0, Math.min(50, normalized.length())));
            fragments.add(normalized.substring(0, Math.min(20, normalized.length())));
        } else if (StringUtils.hasText(normalized)) {
            fragments.add(normalized);
        }
        Matcher matcher = VERIFY_TEXT_FRAGMENT_PATTERN.matcher(content);
        while (matcher.find() && fragments.size() < 8) {
            String fragment = matcher.group();
            fragments.add(fragment.length() > 50 ? fragment.substring(0, 50) : fragment);
        }
        return new ArrayList<>(fragments);
    }

    private String normalizeForVerify(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("[`*_#>\\[\\](){}!~|\\\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> titleSelectorsForSnapshot() {
        List<String> selectors = new ArrayList<>();
        selectors.addAll(TITLE_SELECTORS);
        selectors.add("[class*='title']");
        selectors.add("[id*='title']");
        selectors.add("[placeholder*='标题']");
        return selectors;
    }

    private List<String> contentSelectorsForSnapshot() {
        List<String> selectors = new ArrayList<>();
        selectors.addAll(CONTENT_EDITABLE_SELECTORS);
        selectors.addAll(CONTENT_FALLBACK_SELECTORS);
        selectors.add(".RichText");
        selectors.add(".public-DraftStyleDefault-block");
        selectors.add(".ProseMirror");
        selectors.add("[class*='editor']");
        return selectors;
    }

    private void logContentSnapshot(Page page) {
        try {
            Object snapshot = page.evaluate("""
                    () => {
                      const text = value => (value || '').replace(/\\s+/g, ' ').trim().slice(0, 120);
                      const active = document.activeElement;
                      const activeInfo = active ? {
                        tag: active.tagName || '',
                        placeholder: active.getAttribute?.('placeholder') || '',
                        aria: active.getAttribute?.('aria-label') || '',
                        contenteditable: active.getAttribute?.('contenteditable') || '',
                        text: text(active.innerText || active.textContent || active.value || '')
                      } : {};
                      const editorText = Array.from(document.querySelectorAll('[contenteditable="true"], .ProseMirror, .DraftEditor-editorContainer, [data-slate-editor="true"], textarea'))
                        .map(element => text(element.innerText || element.textContent || element.value || ''))
                        .filter(Boolean)
                        .slice(0, 5);
                      return { activeInfo, editorText };
                    }
                    """);
            log.info("Zhihu content verify snapshot: {}", snapshot);
        } catch (RuntimeException exception) {
            log.warn("Zhihu content verify snapshot failed: reason={}", safeMessage(exception));
        }
    }

    private boolean clickFirst(Page page, List<String> selectors, long deadlineMs) {
        for (String selector : selectors) {
            if (System.currentTimeMillis() >= deadlineMs) {
                return false;
            }
            try {
                page.locator(selector).first().click(new Locator.ClickOptions().setTimeout(boundedTimeout(deadlineMs, TITLE_STRATEGY_TIMEOUT_MS)));
                return true;
            } catch (RuntimeException ignored) {
                // Try the next selector.
            }
        }
        return false;
    }

    private double boundedTimeout(long deadlineMs, double preferredMs) {
        long remainingMs = deadlineMs - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 1;
        }
        return Math.max(1, Math.min(preferredMs, remainingMs));
    }

    private long nextStrategyDeadline(long deadlineMs, long strategyBudgetMs) {
        return Math.min(deadlineMs, System.currentTimeMillis() + strategyBudgetMs);
    }

    private String currentOrConfiguredUrl(Page page, BrowserPublisherConfig config) {
        String currentUrl = browserAutomationService.currentUrl(page);
        return StringUtils.hasText(currentUrl) ? currentUrl : config.editorUrl();
    }

    private String safeUrl(Page page) {
        return browserAutomationService.currentUrl(page);
    }

    private boolean isZhihuPage(Page page) {
        String url = safeUrl(page);
        return StringUtils.hasText(url) && (url.contains("zhihu.com") || url.contains("zhuanlan.zhihu.com"));
    }

    private boolean isZhihuEditorPage(Page page) {
        String url = safeUrl(page);
        return isZhihuPage(page) && (url.contains("/write") || hasEditorDom(page));
    }

    private boolean isZhihuArticleUrl(String url) {
        return StringUtils.hasText(url)
                && !url.contains("/edit")
                && !url.contains("/write")
                && (url.contains("zhuanlan.zhihu.com/p/")
                || (url.contains("zhihu.com") && url.contains("/p/"))
                || (url.contains("zhihu.com/question/") && url.contains("/answer/")));
    }

    private void navigateToManagePage(Page page, BrowserPublisherConfig config) {
        try {
            page.navigate(fallbackManageUrl(config), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(10_000));
        } catch (RuntimeException ignored) {
            // Result detection continues on current page.
        }
    }

    private String fallbackManageUrl(BrowserPublisherConfig config) {
        return StringUtils.hasText(config.manageUrl()) ? config.manageUrl() : DEFAULT_MANAGE_URL;
    }

    private void writeClipboard(Page page, String value) {
        page.evaluate("""
                text => {
                  try {
                    return Promise.race([
                      navigator.clipboard.writeText(text).then(() => true).catch(() => false),
                      new Promise(resolve => setTimeout(() => resolve(false), 500))
                    ]);
                  } catch (e) {
                    return false;
                  }
                }
                """, value);
    }

    private String pageSnapshot(Page page) {
        return browserAutomationService.textSnapshot(page, List.of(
                "body",
                "[role='dialog']",
                "[role='alert']",
                "[class*='Modal']",
                "[class*='modal']",
                "[class*='Toast']",
                "[class*='toast']",
                "[class*='Message']",
                "[class*='message']"
        ), 8_000);
    }

    private boolean containsAny(String value, List<String> needles) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return needles.stream().anyMatch(value::contains);
    }

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String preview(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private FillOutcome titleSuccess(String strategy, long startedAt) {
        log.info("Zhihu title fill succeeded: strategy={}, durationMs={}", strategy, elapsed(startedAt));
        return new FillOutcome(true, false, strategy);
    }

    private FillOutcome titleFailed(String strategy, long startedAt, boolean timedOut) {
        log.warn("Zhihu title fill failed: strategy={}, durationMs={}, timedOut={}", strategy, elapsed(startedAt), timedOut);
        return new FillOutcome(false, timedOut, strategy);
    }

    private ContentFillOutcome contentSuccess(String strategy, long startedAt) {
        log.info("Zhihu content fill succeeded: strategy={}, durationMs={}", strategy, elapsed(startedAt));
        return new ContentFillOutcome(true, false, strategy);
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private void scrollToBottom(Page page) {
        try {
            page.evaluate("() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'instant' })");
        } catch (RuntimeException ignored) {
            // Best effort only.
        }
    }

    private void bringToFront(Page page) {
        try {
            page.bringToFront();
        } catch (RuntimeException ignored) {
            // Best effort only.
        }
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

    private void validateContext(PublishContext context) {
        if (!PLATFORM.equals(context.platform())) {
            throw new BusinessException("ZhihuBrowserPublisher 只支持 ZHIHU 平台");
        }
        if (!MODE_BROWSER_AUTOMATION.equals(context.publishMode())) {
            throw new BusinessException("ZhihuBrowserPublisher 只支持 BROWSER_AUTOMATION 发布准备方式");
        }
        if (!StringUtils.hasText(context.content())) {
            throw new BusinessException("知乎平台稿正文不能为空");
        }
        if (!context.accountConfigExists() || !StringUtils.hasText(context.accountAuthConfig())) {
            throw new BusinessException("知乎平台账号 auth_config 未配置");
        }
    }

    private String sessionKey(PublishContext context) {
        return PLATFORM + ":" + context.accountId();
    }

    private String safeMessage(RuntimeException exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }

    private record LoginDetection(boolean required, String reason) {
    }

    private record CaptchaDetection(boolean required, String reason) {
    }

    private record FillOutcome(boolean success, boolean timedOut, String strategy) {
    }

    private record ContentFillOutcome(boolean success, boolean weakSuccess, String strategy) {
    }
}
