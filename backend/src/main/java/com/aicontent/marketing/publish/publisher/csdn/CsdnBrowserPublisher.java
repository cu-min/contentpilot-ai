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
public class CsdnBrowserPublisher implements PlatformPublisher {

    private static final String PLATFORM = "CSDN";
    private static final String MODE_BROWSER_AUTOMATION = "BROWSER_AUTOMATION";
    private static final String MODE_MANUAL_CONFIRM = "MANUAL_CONFIRM";
    private static final String DEFAULT_EDITOR_URL = "https://editor.csdn.net/md/?not_checkout=1";
    private static final String MANUAL_CONFIRM_MESSAGE = "已自动填充 CSDN 编辑器，请在浏览器中人工确认并发布";
    private static final Logger log = LoggerFactory.getLogger(CsdnBrowserPublisher.class);
    private static final long TITLE_TOTAL_TIMEOUT_MS = 15_000;
    private static final double TITLE_STRATEGY_TIMEOUT_MS = 2_500;
    private static final long TITLE_STRATEGY_BUDGET_MS = 3_000;
    private static final long CONTENT_TOTAL_TIMEOUT_MS = 25_000;
    private static final double CONTENT_STRATEGY_TIMEOUT_MS = 3_000;
    private static final long CONTENT_STRATEGY_BUDGET_MS = 5_000;
    private static final long CONTENT_VERIFY_SETTLE_MS = 1_000;
    private static final int CONTENT_VERIFY_SNAPSHOT_LENGTH = 12_000;
    private static final long OPTIONAL_TOTAL_TIMEOUT_MS = 5_000;
    private static final double OPTIONAL_STRATEGY_TIMEOUT_MS = 800;
    private static final Pattern VERIFY_TEXT_FRAGMENT_PATTERN = Pattern.compile("[\\p{IsHan}A-Za-z0-9]{6,}");
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
    private final ReentrantLock automationLock = new ReentrantLock();

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
        boolean locked = false;
        try {
            locked = automationLock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("CSDN browser automation rejected: another task is running, taskId={}", context.taskId());
                return PublishResult.failed("已有 CSDN 自动化任务正在执行，请完成后再重试");
            }
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
            log.info("CSDN markdown page selected: taskId={}, url={}", context.taskId(), safeUrl(page));

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
            FillOutcome titleOutcome = fillTitle(page, context.title());
            if (!titleOutcome.success()) {
                logTitleCandidates(page, editorType);
                String message = titleOutcome.timedOut()
                        ? "CSDN Markdown 标题填充超时"
                        : "CSDN Markdown 标题填充失败，请人工检查页面结构";
                return PublishResult.failed(message);
            }
            FillOutcome contentOutcome = fillContent(page, context.content());
            if (!contentOutcome.success()) {
                return PublishResult.failed("CSDN Markdown 正文填充失败。请人工检查页面结构");
            }
            if (config.manualConfirm()) {
                log.info("CSDN optional fields skipped: reason=manualConfirm, taskId={}", context.taskId());
                return manualConfirmResult(context, page, config);
            }
            fillOptionalMetadata(page, context, config);
            return manualConfirmResult(context, page, config);
        } catch (BusinessException exception) {
            return PublishResult.failed(exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return PublishResult.failed("CSDN 自动化任务等待执行锁被中断");
        } catch (RuntimeException exception) {
            return PublishResult.linkFetchFailed(null, "CSDN 编辑器页面打开或填充失败：" + safeMessage(exception));
        } finally {
            if (locked) {
                automationLock.unlock();
            }
        }
    }

    private PublishResult manualConfirmResult(PublishContext context, Page page, BrowserPublisherConfig config) {
        String url = currentOrConfiguredUrl(page, config);
        PublishResult result = PublishResult.needManualConfirm(url, url, MANUAL_CONFIRM_MESSAGE);
        log.info("CSDN manual confirm result returned: taskId={}, url={}", context.taskId(), url);
        log.info("CSDN publish task result status = NEED_MANUAL_CONFIRM");
        return result;
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

    private FillOutcome fillTitle(Page page, String title) {
        String normalizedTitle = normalize(title, "未命名 CSDN 草稿");
        long startedAt = System.currentTimeMillis();
        long deadlineMs = startedAt + TITLE_TOTAL_TIMEOUT_MS;
        int candidateCount = browserAutomationService.elementSummaries(page, TITLE_CLICK_SELECTORS, 20).size();
        log.info("CSDN title fill started: url={}, editorType={}, candidateCount={}, titleLength={}",
                safeUrl(page), detectEditorType(page), candidateCount, normalizedTitle.length());
        if (browserAutomationService.fillFirstInPageOrFramesWithin(page, TITLE_VALUE_SELECTORS, normalizedTitle,
                TITLE_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS))
                && titleFilled(page, normalizedTitle)) {
            return titleSuccess("fill", startedAt);
        }
        if (titleTimedOut(startedAt)) {
            return titleFailed("fill", startedAt, true);
        }
        if (browserAutomationService.clickAndInsertFirstInPageOrFramesWithin(page, TITLE_CLICK_SELECTORS, normalizedTitle,
                TITLE_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS))
                && titleFilled(page, normalizedTitle)) {
            return titleSuccess("click-insert", startedAt);
        }
        if (titleTimedOut(startedAt)) {
            return titleFailed("click-insert", startedAt, true);
        }
        boolean pasted = browserAutomationService.pasteFirstInPageOrFramesWithin(page, TITLE_CLICK_SELECTORS, normalizedTitle,
                TITLE_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS))
                && titleFilled(page, normalizedTitle);
        if (pasted) {
            return titleSuccess("paste", startedAt);
        }
        if (titleTimedOut(startedAt)) {
            return titleFailed("paste", startedAt, true);
        }
        boolean coordinateInserted = browserAutomationService.clickAtAndInsert(page, 300, 135, normalizedTitle)
                && titleFilled(page, normalizedTitle);
        if (coordinateInserted) {
            return titleSuccess("coordinate-insert", startedAt);
        }
        return titleFailed("coordinate-insert", startedAt, titleTimedOut(startedAt));
    }

    private FillOutcome fillContent(Page page, String content) {
        String normalizedContent = normalize(content, "");
        long startedAt = System.currentTimeMillis();
        long deadlineMs = startedAt + CONTENT_TOTAL_TIMEOUT_MS;
        String contentPreview = preview(normalizedContent);
        log.info("CSDN content fill started: url={}, editorType={}, contentLength={}, contentPreview={}",
                safeUrl(page), detectEditorType(page), normalizedContent.length(), contentPreview);
        if (attemptContentFill(page, normalizedContent, "coordinate-paste", startedAt,
                () -> browserAutomationService.clickAtAndPaste(page, 80, 360, normalizedContent))) {
            return contentSuccess("coordinate-paste", startedAt);
        }
        if (contentTimedOut(startedAt)) {
            return contentFailed("coordinate-paste", startedAt);
        }
        if (attemptContentFill(page, normalizedContent, "paste-editable", startedAt,
                () -> browserAutomationService.pasteFirstInPageOrFramesWithin(page, CONTENT_EDITABLE_SELECTORS, normalizedContent,
                        CONTENT_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, CONTENT_STRATEGY_BUDGET_MS)))) {
            return contentSuccess("paste-editable", startedAt);
        }
        if (contentTimedOut(startedAt)) {
            return contentFailed("paste-editable", startedAt);
        }
        if (attemptContentFill(page, normalizedContent, "paste-textarea", startedAt,
                () -> browserAutomationService.pasteFirstInPageOrFramesWithin(page, CONTENT_FILL_SELECTORS, normalizedContent,
                        CONTENT_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, CONTENT_STRATEGY_BUDGET_MS)))) {
            return contentSuccess("paste-textarea", startedAt);
        }
        if (contentTimedOut(startedAt)) {
            return contentFailed("paste-textarea", startedAt);
        }
        if (attemptContentFill(page, normalizedContent, "fill", startedAt,
                () -> browserAutomationService.fillFirstInPageOrFramesWithin(page, CONTENT_FILL_SELECTORS, normalizedContent,
                        CONTENT_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, CONTENT_STRATEGY_BUDGET_MS)))) {
            return contentSuccess("fill", startedAt);
        }
        if (contentTimedOut(startedAt)) {
            return contentFailed("fill", startedAt);
        }
        if (attemptContentFill(page, normalizedContent, "click-insert", startedAt,
                () -> browserAutomationService.clickAndInsertFirstInPageOrFramesWithin(page, CONTENT_EDITABLE_SELECTORS, normalizedContent,
                        CONTENT_STRATEGY_TIMEOUT_MS, nextStrategyDeadline(deadlineMs, CONTENT_STRATEGY_BUDGET_MS)))) {
            return contentSuccess("click-insert", startedAt);
        }
        return contentFailed("click-insert", startedAt);
    }

    private boolean attemptContentFill(Page page, String content, String strategy, long startedAt, BooleanSupplier action) {
        String contentPreview = preview(content);
        log.info("CSDN content paste attempted: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, content.length(), contentPreview, elapsed(startedAt));
        boolean attempted;
        try {
            attempted = action.getAsBoolean();
        } catch (RuntimeException exception) {
            log.warn("CSDN content paste finished: strategy={}, success=false, contentLength={}, contentPreview={}, durationMs={}, reason={}",
                    strategy, content.length(), contentPreview, elapsed(startedAt), safeMessage(exception));
            return false;
        }
        log.info("CSDN content paste finished: strategy={}, success={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, attempted, content.length(), contentPreview, elapsed(startedAt));
        if (!attempted) {
            return false;
        }
        sleep(CONTENT_VERIFY_SETTLE_MS);
        log.info("CSDN content verify started: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, content.length(), contentPreview, elapsed(startedAt));
        boolean verified = contentFilled(page, content);
        if (verified) {
            log.info("CSDN content verify succeeded: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                    strategy, content.length(), contentPreview, elapsed(startedAt));
            return true;
        }
        log.warn("CSDN content verify failed: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                strategy, content.length(), contentPreview, elapsed(startedAt));
        if (isMarkdownEditorPage(page) && StringUtils.hasText(content)) {
            log.warn("CSDN content verification weakly passed after paste: strategy={}, contentLength={}, contentPreview={}, durationMs={}",
                    strategy, content.length(), contentPreview, elapsed(startedAt));
            return true;
        }
        return false;
    }

    private void fillOptionalMetadata(Page page, PublishContext context, BrowserPublisherConfig config) {
        long startedAt = System.currentTimeMillis();
        long deadlineMs = startedAt + OPTIONAL_TOTAL_TIMEOUT_MS;
        log.info("CSDN optional fields fill started: url={}", safeUrl(page));
        if ((config.defaultTags() == null || config.defaultTags().isEmpty())
                && !StringUtils.hasText(config.defaultSummary())
                && !StringUtils.hasText(context.summary())
                && !StringUtils.hasText(config.defaultCategory())
                && !StringUtils.hasText(config.defaultColumn())) {
            log.info("CSDN optional fields skipped: reason=no-config, durationMs={}", elapsed(startedAt));
            return;
        }
        try {
            browserAutomationService.fillTagLikeInputsWithin(page, config.defaultTags(), List.of(
                    "input[placeholder*='文章标签']",
                    "input[placeholder*='标签']",
                    "input[aria-label*='标签']"
            ), OPTIONAL_STRATEGY_TIMEOUT_MS, deadlineMs);
            String summary = StringUtils.hasText(config.defaultSummary())
                    ? config.defaultSummary()
                    : normalize(context.summary(), "");
            if (StringUtils.hasText(summary) && !optionalTimedOut(startedAt)) {
                browserAutomationService.fillFirstInPageOrFramesWithin(page, List.of(
                        "textarea[placeholder*='摘要']",
                        "textarea[placeholder*='简介']",
                        "textarea[placeholder*='描述']",
                        "input[placeholder*='摘要']",
                        "[aria-label*='摘要']"
                ), summary, OPTIONAL_STRATEGY_TIMEOUT_MS, deadlineMs);
            }
            if (StringUtils.hasText(config.defaultCategory()) && !optionalTimedOut(startedAt)) {
                browserAutomationService.fillFirstInPageOrFramesWithin(page, List.of(
                        "input[placeholder*='文章分类']",
                        "input[placeholder*='分类']",
                        "input[aria-label*='分类']",
                        "[role='combobox'][aria-label*='分类']"
                ), config.defaultCategory(), OPTIONAL_STRATEGY_TIMEOUT_MS, deadlineMs);
            }
            if (StringUtils.hasText(config.defaultColumn()) && !optionalTimedOut(startedAt)) {
                browserAutomationService.fillFirstInPageOrFramesWithin(page, List.of(
                        "input[placeholder*='专栏']",
                        "input[aria-label*='专栏']",
                        "[role='combobox'][aria-label*='专栏']"
                ), config.defaultColumn(), OPTIONAL_STRATEGY_TIMEOUT_MS, deadlineMs);
            }
            log.info("CSDN optional fields succeeded: durationMs={}", elapsed(startedAt));
        } catch (RuntimeException exception) {
            log.warn("CSDN optional fields failed: durationMs={}, reason={}", elapsed(startedAt), safeMessage(exception));
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
        if (!StringUtils.hasText(content)) {
            return true;
        }
        String snapshot = browserAutomationService.textSnapshot(page, contentSelectorsForSnapshot(), CONTENT_VERIFY_SNAPSHOT_LENGTH);
        return snapshotContainsContent(snapshot, content);
    }

    private List<String> contentSelectorsForSnapshot() {
        List<String> selectors = new ArrayList<>();
        selectors.addAll(CONTENT_FILL_SELECTORS);
        selectors.addAll(CONTENT_EDITABLE_SELECTORS);
        selectors.add(".cm-line");
        selectors.add(".cm-content");
        selectors.add(".editor-preview");
        selectors.add(".markdown-body");
        selectors.add("[class*='preview']");
        return selectors;
    }

    private boolean snapshotContainsContent(String snapshot, String content) {
        if (!StringUtils.hasText(snapshot)) {
            return false;
        }
        String normalizedSnapshot = normalizeForVerify(snapshot);
        for (String fragment : verifyFragments(content)) {
            if (normalizedSnapshot.contains(normalizeForVerify(fragment))) {
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
        }
        Matcher matcher = VERIFY_TEXT_FRAGMENT_PATTERN.matcher(content);
        while (matcher.find() && fragments.size() < 8) {
            String fragment = matcher.group();
            if (fragment.length() > 50) {
                fragment = fragment.substring(0, 50);
            }
            fragments.add(fragment);
        }
        return new ArrayList<>(fragments);
    }

    private String normalizeForVerify(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replaceAll("[`*_#>\\[\\](){}!~|\\\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String currentOrConfiguredUrl(Page page, BrowserPublisherConfig config) {
        String currentUrl = browserAutomationService.currentUrl(page);
        return StringUtils.hasText(currentUrl) ? currentUrl : config.editorUrl();
    }

    private String safeUrl(Page page) {
        return browserAutomationService.currentUrl(page);
    }

    private boolean isMarkdownEditorPage(Page page) {
        String url = browserAutomationService.currentUrl(page);
        return StringUtils.hasText(url) && url.contains("editor.csdn.net/md");
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

    private FillOutcome titleSuccess(String strategy, long startedAt) {
        log.info("CSDN title fill succeeded: strategy={}, durationMs={}", strategy, elapsed(startedAt));
        return new FillOutcome(true, false, strategy);
    }

    private FillOutcome titleFailed(String strategy, long startedAt, boolean timedOut) {
        log.warn("CSDN title fill failed: strategy={}, durationMs={}, timedOut={}", strategy, elapsed(startedAt), timedOut);
        return new FillOutcome(false, timedOut, strategy);
    }

    private FillOutcome contentSuccess(String strategy, long startedAt) {
        log.info("CSDN content fill succeeded: strategy={}, durationMs={}", strategy, elapsed(startedAt));
        return new FillOutcome(true, false, strategy);
    }

    private FillOutcome contentFailed(String strategy, long startedAt) {
        boolean timedOut = contentTimedOut(startedAt);
        log.warn("CSDN content fill failed: strategy={}, durationMs={}, timedOut={}", strategy, elapsed(startedAt), timedOut);
        return new FillOutcome(false, timedOut, strategy);
    }

    private boolean titleTimedOut(long startedAt) {
        return elapsed(startedAt) >= TITLE_TOTAL_TIMEOUT_MS;
    }

    private boolean contentTimedOut(long startedAt) {
        return elapsed(startedAt) >= CONTENT_TOTAL_TIMEOUT_MS;
    }

    private boolean optionalTimedOut(long startedAt) {
        return elapsed(startedAt) >= OPTIONAL_TOTAL_TIMEOUT_MS;
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private long nextStrategyDeadline(long totalDeadlineMs, long strategyBudgetMs) {
        return Math.min(totalDeadlineMs, System.currentTimeMillis() + strategyBudgetMs);
    }

    private String preview(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 20 ? compact.substring(0, 20) : compact;
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

    private record FillOutcome(boolean success, boolean timedOut, String strategy) {
    }
}
