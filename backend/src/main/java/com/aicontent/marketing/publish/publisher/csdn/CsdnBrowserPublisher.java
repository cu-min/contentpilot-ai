package com.aicontent.marketing.publish.publisher.csdn;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.publisher.browser.BrowserAutomationRequest;
import com.aicontent.marketing.publish.publisher.browser.BrowserAutomationService;
import com.aicontent.marketing.publish.publisher.browser.BrowserAutomationSession;
import com.aicontent.marketing.publish.publisher.browser.BrowserPublisherConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class CsdnBrowserPublisher implements PlatformPublisher {

    private static final String PLATFORM = "CSDN";
    private static final String MODE_BROWSER_AUTOMATION = "BROWSER_AUTOMATION";
    private static final String MODE_MANUAL_CONFIRM = "MANUAL_CONFIRM";
    private static final String DEFAULT_EDITOR_URL = "https://editor.csdn.net/md/?not_checkout=1";
    private static final String MANUAL_CONFIRM_MESSAGE = "已自动填充 CSDN 编辑器，请在浏览器中人工确认并发布";
    private static final Logger log = LoggerFactory.getLogger(CsdnBrowserPublisher.class);
    private static final List<String> TITLE_VALUE_SELECTORS = List.of(
            "input[placeholder*='请输入文章标题']",
            "textarea[placeholder*='请输入文章标题']",
            "input[placeholder*='标题']",
            "textarea[placeholder*='标题']",
            "input[name='title']",
            "[contenteditable='true'][placeholder*='请输入文章标题']",
            "[contenteditable='true'][aria-label*='标题']",
            "[role='textbox'][aria-label*='标题']",
            "#txtTitle"
    );
    private static final List<String> TITLE_CLICK_SELECTORS = List.of(
            "text=【无标题】",
            "text=无标题",
            "[title*='无标题']",
            "[aria-label*='无标题']",
            "input[placeholder*='请输入文章标题']",
            "textarea[placeholder*='请输入文章标题']",
            "[contenteditable='true'][placeholder*='请输入文章标题']",
            "[contenteditable='true'][aria-label*='标题']",
            "[role='textbox'][aria-label*='标题']",
            "text=请输入文章标题",
            "text=请输入文章标题（5~100个字）"
    );
    private static final List<String> CONTENT_FILL_SELECTORS = List.of(
            "textarea:not([placeholder*='标题'])",
            "textarea[placeholder*='正文']",
            "textarea[placeholder*='开始创作']",
            "textarea[placeholder*='请输入正文']",
            "textarea.markdown-editor",
            "textarea#md-editor",
            ".bytemd-editor textarea",
            ".editor textarea",
            ".CodeMirror textarea",
            ".monaco-editor textarea"
    );
    private static final List<String> CONTENT_EDITABLE_SELECTORS = List.of(
            "[contenteditable='true']:not([placeholder*='标题'])",
            "[role='textbox']:not([aria-label*='标题'])",
            ".cm-content",
            ".CodeMirror-code",
            ".monaco-editor textarea"
    );
    private static final List<String> MARKDOWN_READY_SELECTORS = List.of(
            "text=Markdown",
            "text=比对",
            "text=预览",
            "text=【无标题】",
            "text=无标题",
            "textarea[placeholder*='正文']",
            "textarea[placeholder*='开始创作']",
            "[contenteditable='true']",
            ".cm-content",
            ".CodeMirror-code",
            ".monaco-editor",
            "textarea"
    );
    private static final List<String> RICH_TEXT_SWITCH_SELECTORS = List.of(
            "button:has-text('使用 MD 编辑器')",
            "text=使用 MD 编辑器"
    );
    private static final List<String> DRAFT_PROMPT_CLOSE_SELECTORS = List.of(
            "button:has-text('关闭')",
            "button:has-text('取消')",
            "[aria-label='关闭']"
    );

    private final BrowserAutomationService browserAutomationService;
    private final ObjectMapper objectMapper;

    public CsdnBrowserPublisher(BrowserAutomationService browserAutomationService, ObjectMapper objectMapper) {
        this.browserAutomationService = browserAutomationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public String mode() {
        return "*";
    }

    @Override
    public PublishResult publish(PublishContext context) {
        try {
            validateContext(context);
            BrowserPublisherConfig config = BrowserPublisherConfig.parse(
                    context.accountAuthConfig(),
                    objectMapper,
                    DEFAULT_EDITOR_URL
            );
            BrowserAutomationSession session = browserAutomationService.openSession(new BrowserAutomationRequest(
                    sessionKey(context),
                    config.browserUserDataDir(),
                    config.editorUrl(),
                    config.headless(),
                    config.timeoutMs()
            ));
            Page page = resolveMarkdownPage(session, config.timeoutMs());
            if (isLoginPage(page)) {
                return PublishResult.needLogin(currentOrConfiguredUrl(page, config), "CSDN 未检测到登录态，请在打开的浏览器中完成登录后重新执行发布任务");
            }

            EditorType editorType = detectEditorType(page);
            log.info("CSDN editor automation ready check: url={}, editorType={}", safeUrl(page), editorType);
            if (editorType == EditorType.RICH_TEXT && !switchToMarkdownEditor(session, page, config.timeoutMs())) {
                log.warn("CSDN editor automation failed to switch editor: url={}, editorType={}", safeUrl(page), editorType);
                return PublishResult.failed("未能切换到 CSDN Markdown 编辑器");
            }
            page = resolveMarkdownPage(session, config.timeoutMs());
            editorType = detectEditorType(page);
            if (editorType != EditorType.MARKDOWN) {
                log.warn("CSDN editor automation did not reach markdown editor: url={}, editorType={}", safeUrl(page), editorType);
                return PublishResult.failed("未能切换到 CSDN Markdown 编辑器");
            }

            if (!waitForMarkdownEditorReady(page, config.timeoutMs())) {
                if (isLoginPage(page) || browserAutomationService.looksLoggedOut(page)) {
                    return PublishResult.needLogin(currentOrConfiguredUrl(page, config), "CSDN 未检测到登录态，请在打开的浏览器中完成登录后重新执行发布任务");
                }
                if (browserAutomationService.looksCaptchaBlocked(page)) {
                    return PublishResult.needCaptcha(currentOrConfiguredUrl(page, config), "CSDN 页面需要人工完成验证码或安全验证后重新执行发布任务");
                }
                logTitleCandidates(page, editorType);
                return PublishResult.failed("CSDN Markdown 编辑器未检测到标题或正文可编辑区域，自动填充失败，请人工检查页面结构");
            }
            if (browserAutomationService.looksCaptchaBlocked(page)) {
                return PublishResult.needCaptcha(currentOrConfiguredUrl(page, config), "CSDN 页面需要人工完成验证码或安全验证后重新执行发布任务");
            }
            dismissDraftPrompt(page);
            if (!fillTitle(page, context.title(), config.timeoutMs())) {
                logTitleCandidates(page, editorType);
                return PublishResult.failed("CSDN Markdown 编辑器标题填充失败，请人工检查页面结构");
            }
            if (!fillContent(page, context.content(), config.timeoutMs())) {
                return PublishResult.failed("CSDN Markdown 编辑器正文填充失败，请人工检查页面结构");
            }
            fillOptionalMetadata(page, context, config);
            return PublishResult.needManualConfirm(
                    currentOrConfiguredUrl(page, config),
                    currentOrConfiguredUrl(page, config),
                    MANUAL_CONFIRM_MESSAGE
            );
        } catch (BusinessException exception) {
            return PublishResult.failed(exception.getMessage());
        } catch (RuntimeException exception) {
            return PublishResult.linkFetchFailed(null, "CSDN 编辑器页面打开或填充失败：" + safeMessage(exception));
        }
    }

    private void validateContext(PublishContext context) {
        if (!PLATFORM.equals(context.platform())) {
            throw new BusinessException("CsdnBrowserPublisher 只支持 CSDN 平台");
        }
        if (!MODE_BROWSER_AUTOMATION.equals(context.publishMode()) && !MODE_MANUAL_CONFIRM.equals(context.publishMode())) {
            throw new BusinessException("CsdnBrowserPublisher 只支持 BROWSER_AUTOMATION 或 MANUAL_CONFIRM 发布方式");
        }
        if (!StringUtils.hasText(context.content())) {
            throw new BusinessException("CSDN 平台稿正文不能为空");
        }
        if (!context.accountConfigExists() || !StringUtils.hasText(context.accountAuthConfig())) {
            throw new BusinessException("CSDN 平台账号 auth_config 未配置");
        }
    }

    private boolean fillTitle(Page page, String title, double timeoutMs) {
        String normalizedTitle = normalize(title, "未命名 CSDN 草稿");
        int candidateCount = browserAutomationService.elementSummaries(page, TITLE_CLICK_SELECTORS, 20).size();
        log.info("CSDN title fill started: url={}, editorType={}, candidateCount={}",
                safeUrl(page), detectEditorType(page), candidateCount);
        if (browserAutomationService.fillFirstInPageOrFrames(page, TITLE_VALUE_SELECTORS, normalizedTitle, timeoutMs)
                && titleFilled(page, normalizedTitle)) {
            log.info("CSDN title fill succeeded: strategy=fill, verified=true");
            return true;
        }
        if (browserAutomationService.clickAndInsertFirstInPageOrFrames(page, TITLE_CLICK_SELECTORS, normalizedTitle, timeoutMs)
                && titleFilled(page, normalizedTitle)) {
            log.info("CSDN title fill succeeded: strategy=click-insert, verified=true");
            return true;
        }
        boolean pasted = browserAutomationService.pasteFirstInPageOrFrames(page, TITLE_CLICK_SELECTORS, normalizedTitle, timeoutMs)
                && titleFilled(page, normalizedTitle);
        if (pasted) {
            log.info("CSDN title fill succeeded: strategy=paste, verified=true");
            return true;
        }
        boolean coordinateInserted = browserAutomationService.clickAtAndInsert(page, 300, 135, normalizedTitle)
                && titleFilled(page, normalizedTitle);
        log.info("CSDN title fill finished: strategy=coordinate-insert, verified={}", coordinateInserted);
        return coordinateInserted;
    }

    private boolean fillContent(Page page, String content, double timeoutMs) {
        String normalizedContent = normalize(content, "");
        if (browserAutomationService.clickAtAndPaste(page, 80, 360, normalizedContent)
                && contentFilled(page, normalizedContent)) {
            log.info("CSDN content fill succeeded: strategy=coordinate-paste, verified=true");
            return true;
        }
        if (browserAutomationService.pasteFirstInPageOrFrames(page, CONTENT_EDITABLE_SELECTORS, normalizedContent, timeoutMs)
                && contentFilled(page, normalizedContent)) {
            log.info("CSDN content fill succeeded: strategy=paste-editable, verified=true");
            return true;
        }
        if (browserAutomationService.pasteFirstInPageOrFrames(page, CONTENT_FILL_SELECTORS, normalizedContent, timeoutMs)
                && contentFilled(page, normalizedContent)) {
            log.info("CSDN content fill succeeded: strategy=paste-textarea, verified=true");
            return true;
        }
        if (browserAutomationService.fillFirstInPageOrFrames(page, CONTENT_FILL_SELECTORS, normalizedContent, timeoutMs)
                && contentFilled(page, normalizedContent)) {
            log.info("CSDN content fill succeeded: strategy=fill, verified=true");
            return true;
        }
        if (browserAutomationService.clickAndInsertFirstInPageOrFrames(page, CONTENT_EDITABLE_SELECTORS, normalizedContent, timeoutMs)
                && contentFilled(page, normalizedContent)) {
            log.info("CSDN content fill succeeded: strategy=click-insert, verified=true");
            return true;
        }
        log.warn("CSDN content fill failed: url={}, editorType={}", safeUrl(page), detectEditorType(page));
        return false;
    }

    private void fillOptionalMetadata(Page page, PublishContext context, BrowserPublisherConfig config) {
        browserAutomationService.fillTagLikeInputs(page, config.defaultTags(), List.of(
                "input[placeholder*='文章标签']",
                "input[placeholder*='标签']",
                "input[aria-label*='标签']"
        ), 1_500);
        String summary = StringUtils.hasText(config.defaultSummary())
                ? config.defaultSummary()
                : normalize(context.summary(), "");
        if (StringUtils.hasText(summary)) {
            browserAutomationService.fillFirstInPageOrFrames(page, List.of(
                    "textarea[placeholder*='摘要']",
                    "textarea[placeholder*='简介']",
                    "textarea[placeholder*='描述']",
                    "input[placeholder*='摘要']",
                    "[aria-label*='摘要']"
            ), summary, 1_500);
        }
        if (StringUtils.hasText(config.defaultCategory())) {
            browserAutomationService.fillFirstInPageOrFrames(page, List.of(
                    "input[placeholder*='文章分类']",
                    "input[placeholder*='分类']",
                    "input[aria-label*='分类']",
                    "[role='combobox'][aria-label*='分类']"
            ), config.defaultCategory(), 1_500);
        }
        // CSDN 专栏入口在不同账号/编辑器版本中差异较大；仅在可见输入框存在时尝试填充。
        if (StringUtils.hasText(config.defaultColumn())) {
            browserAutomationService.fillFirstInPageOrFrames(page, List.of(
                    "input[placeholder*='专栏']",
                    "input[aria-label*='专栏']",
                    "[role='combobox'][aria-label*='专栏']"
            ), config.defaultColumn(), 1_500);
        }
    }

    private String sessionKey(PublishContext context) {
        return PLATFORM + ":" + context.accountId();
    }

    private Page resolveMarkdownPage(BrowserAutomationSession session, double timeoutMs) {
        Page markdownPage = browserAutomationService.latestPageMatching(session, "editor.csdn.net/md");
        if (markdownPage != null) {
            bringToFront(markdownPage);
            return markdownPage;
        }
        Page currentPage = session.page();
        long deadline = System.currentTimeMillis() + Math.max(1_000, (long) timeoutMs / 3);
        while (System.currentTimeMillis() < deadline) {
            markdownPage = browserAutomationService.latestPageMatching(session, "editor.csdn.net/md");
            if (markdownPage != null) {
                bringToFront(markdownPage);
                return markdownPage;
            }
            sleep(200);
        }
        return currentPage;
    }

    private boolean switchToMarkdownEditor(BrowserAutomationSession session, Page richTextPage, double timeoutMs) {
        if (browserAutomationService.clickFirstInPageOrFrames(richTextPage, RICH_TEXT_SWITCH_SELECTORS, 2_000)
                || browserAutomationService.clickTextInPageOrFrames(richTextPage, List.of("使用 MD 编辑器"), 2_000)) {
            long deadline = System.currentTimeMillis() + Math.max(5_000, (long) timeoutMs);
            while (System.currentTimeMillis() < deadline) {
                Page markdownPage = browserAutomationService.latestPageMatching(session, "editor.csdn.net/md");
                if (markdownPage != null) {
                    bringToFront(markdownPage);
                    return true;
                }
                if (detectEditorType(richTextPage) == EditorType.MARKDOWN) {
                    return true;
                }
                sleep(300);
            }
        }
        return false;
    }

    private boolean waitForMarkdownEditorReady(Page page, double timeoutMs) {
        long deadline = System.currentTimeMillis() + Math.max(5_000, (long) timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            if (detectEditorType(page) == EditorType.MARKDOWN
                    && browserAutomationService.anyVisibleInPageOrFrames(page, MARKDOWN_READY_SELECTORS)) {
                return true;
            }
            sleep(300);
        }
        return false;
    }

    private EditorType detectEditorType(Page page) {
        String url = browserAutomationService.currentUrl(page);
        if (StringUtils.hasText(url) && url.contains("editor.csdn.net/md")) {
            return EditorType.MARKDOWN;
        }
        if (StringUtils.hasText(url) && url.contains("mp.csdn.net/mp_blog/creation/editor")) {
            return EditorType.RICH_TEXT;
        }
        if (browserAutomationService.anyVisibleInPageOrFrames(page, List.of("text=使用 MD 编辑器"))) {
            return EditorType.RICH_TEXT;
        }
        if (browserAutomationService.anyVisibleInPageOrFrames(page, List.of("text=Markdown", "text=比对", "text=预览"))) {
            return EditorType.MARKDOWN;
        }
        return EditorType.UNKNOWN;
    }

    private boolean isLoginPage(Page page) {
        String url = browserAutomationService.currentUrl(page);
        return StringUtils.hasText(url) && url.contains("passport.csdn.net/login");
    }

    private boolean titleFilled(Page page, String title) {
        return browserAutomationService.selectorsContainTextOrValue(page, TITLE_VALUE_SELECTORS, title)
                || browserAutomationService.containsTextInPageOrFrames(page, title);
    }

    private boolean contentFilled(Page page, String content) {
        return browserAutomationService.selectorsContainTextOrValue(page, CONTENT_FILL_SELECTORS, content)
                || browserAutomationService.selectorsContainTextOrValue(page, CONTENT_EDITABLE_SELECTORS, content)
                || browserAutomationService.containsTextInPageOrFrames(page, content);
    }

    private String currentOrConfiguredUrl(Page page, BrowserPublisherConfig config) {
        String currentUrl = browserAutomationService.currentUrl(page);
        return StringUtils.hasText(currentUrl) ? currentUrl : config.editorUrl();
    }

    private String safeUrl(Page page) {
        return browserAutomationService.currentUrl(page);
    }

    private void bringToFront(Page page) {
        try {
            page.bringToFront();
        } catch (RuntimeException ignored) {
            // Best effort only.
        }
    }

    private void dismissDraftPrompt(Page page) {
        if (browserAutomationService.anyVisibleInPageOrFrames(page, List.of("text=继续编辑", "text=更多草稿"))) {
            browserAutomationService.clickFirstInPageOrFrames(page, DRAFT_PROMPT_CLOSE_SELECTORS, 800);
        }
    }

    private void logTitleCandidates(Page page, EditorType editorType) {
        List<String> summaries = browserAutomationService.elementSummaries(page, TITLE_CLICK_SELECTORS, 8);
        log.warn("CSDN title candidates: url={}, editorType={}, count={}, items={}",
                safeUrl(page), editorType, summaries.size(), summaries);
    }

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String safeMessage(RuntimeException exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }

    private enum EditorType {
        MARKDOWN,
        RICH_TEXT,
        UNKNOWN
    }
}
