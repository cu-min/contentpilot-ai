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
public class CsdnBrowserPublisher implements PlatformPublisher {

    private static final String PLATFORM = "CSDN";
    private static final String MODE_BROWSER_AUTOMATION = "BROWSER_AUTOMATION";
    private static final String MODE_MANUAL_CONFIRM = "MANUAL_CONFIRM";
    private static final String DEFAULT_EDITOR_URL = "https://editor.csdn.net/md/?not_checkout=1";
    private static final String DEFAULT_MANAGE_URL = "https://mp.csdn.net/mp_blog/manage/article";
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
    private static final long PUBLISH_RESULT_TIMEOUT_MS = 45_000;
    private static final long PUBLISH_CONFIRM_TIMEOUT_MS = 8_000;
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
    private static final List<String> TITLE_DIRECT_INPUT_SELECTORS = List.of(
            "input[placeholder*='请输入文章标题']",
            "textarea[placeholder*='请输入文章标题']"
    );
    private static final List<String> TITLE_HIDDEN_INPUT_SELECTORS = List.of(
            "input.article-bar__title-input",
            "input[placeholder*='请输入文章标题']",
            "input.article-bar__title"
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
    private static final List<String> PUBLISH_BUTTON_SELECTORS = List.of(
            "button:has-text('发布文章')",
            "text=发布文章",
            "button:has-text('立即发布')",
            "text=立即发布",
            "button:has-text('发布')"
    );
    private static final List<String> FINAL_CONFIRM_SELECTORS = List.of(
            "button:has-text('确认发布')",
            "button:has-text('提交发布')",
            "button:has-text('立即发布')",
            "button:has-text('发布文章')",
            "button:has-text('发布')"
    );
    private static final List<String> PUBLISH_SUMMARY_SELECTORS = List.of(
            "textarea[placeholder*='简介']",
            "textarea[placeholder*='摘要']",
            "textarea[placeholder*='描述']",
            "textarea[placeholder*='请输入简介']",
            "textarea[placeholder*='请输入摘要']",
            "textarea[placeholder*='文章简介']",
            "input[placeholder*='简介']",
            "input[placeholder*='摘要']",
            "input[aria-label*='简介']",
            "input[aria-label*='摘要']",
            "[aria-label*='简介']",
            "[aria-label*='摘要']"
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
                    DEFAULT_EDITOR_URL,
                    DEFAULT_MANAGE_URL
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
                return PublishResult.failed("CSDN Markdown 标题填充失败，未能激活标题区域");
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
            if (config.autoPublish()) {
                PublishResult result = autoPublish(context, page, config);
                log.info("CSDN publish task result status = {}", result.taskStatus());
                return result;
            }
            log.info("CSDN auto publish disabled: taskId={}", context.taskId());
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

    private PublishResult autoPublish(PublishContext context, Page page, BrowserPublisherConfig config) {
        long startedAt = System.currentTimeMillis();
        log.info("CSDN publish click started: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
        if (isLoginPage(page) || browserAutomationService.looksLoggedOut(page)) {
            log.warn("CSDN publish result login: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
            return PublishResult.needLogin(currentOrConfiguredUrl(page, config), "CSDN 登录态失效，请在打开的浏览器中完成登录后重新执行发布任务");
        }
        if (browserAutomationService.looksCaptchaBlocked(page)) {
            log.warn("CSDN publish result captcha: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
            return PublishResult.needCaptcha(currentOrConfiguredUrl(page, config), "CSDN 页面需要人工完成验证码或安全验证后重新执行发布任务");
        }
        if (!isMarkdownEditorPage(page)) {
            log.warn("CSDN publish result failed: taskId={}, reason=not-editor, currentUrl={}", context.taskId(), safeUrl(page));
            return PublishResult.failed("CSDN 发布失败：当前页面不是 Markdown 编辑器");
        }
        if (!clickPublishButton(page)) {
            log.warn("CSDN publish result failed: taskId={}, reason=button-not-found, durationMs={}", context.taskId(), elapsed(startedAt));
            return PublishResult.failed("CSDN 发布失败：未找到“发布文章”按钮");
        }
        handlePublishConfirmations(page, context, config);
        log.info("CSDN publish result check started: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
        PublishResult result = waitForPublishResult(page, context, config, startedAt);
        logPublishResult(context, result);
        return result;
    }

    private boolean clickPublishButton(Page page) {
        scrollToBottom(page);
        boolean visible = browserAutomationService.anyVisibleInPageOrFrames(page, PUBLISH_BUTTON_SELECTORS);
        if (visible) {
            log.info("CSDN publish button found: currentUrl={}", safeUrl(page));
        }
        boolean clicked = browserAutomationService.clickFirstInPageOrFrames(page, PUBLISH_BUTTON_SELECTORS, 2_000);
        if (clicked) {
            log.info("CSDN publish button clicked: currentUrl={}", safeUrl(page));
            return true;
        }
        scrollToBottom(page);
        clicked = browserAutomationService.clickFirstInPageOrFrames(page, PUBLISH_BUTTON_SELECTORS, 2_000);
        if (clicked) {
            log.info("CSDN publish button found: currentUrl={}", safeUrl(page));
            log.info("CSDN publish button clicked: currentUrl={}", safeUrl(page));
        }
        return clicked;
    }

    private void handlePublishConfirmations(Page page, PublishContext context, BrowserPublisherConfig config) {
        long deadline = System.currentTimeMillis() + PUBLISH_CONFIRM_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (isLoginPage(page) || browserAutomationService.looksCaptchaBlocked(page)) {
                return;
            }
            fillOptionalMetadata(page, context, config);
            fillPublishDialogRequiredFields(page, context, config);
            if (browserAutomationService.clickFirstInPageOrFrames(page, FINAL_CONFIRM_SELECTORS, 1_000)) {
                log.info("CSDN publish button clicked: stage=final-confirm, currentUrl={}", safeUrl(page));
                sleep(1_000);
                continue;
            }
            sleep(500);
        }
    }

    private void fillPublishDialogRequiredFields(Page page, PublishContext context, BrowserPublisherConfig config) {
        long startedAt = System.currentTimeMillis();
        String summary = resolvePublishSummary(context, config);
        boolean summaryFilled = false;
        if (StringUtils.hasText(summary)) {
            summaryFilled = browserAutomationService.fillFirstInPageOrFramesWithin(
                    page,
                    PUBLISH_SUMMARY_SELECTORS,
                    summary,
                    OPTIONAL_STRATEGY_TIMEOUT_MS,
                    System.currentTimeMillis() + 1_500
            );
        }
        boolean originalChecked = checkOriginalStatement(page);
        log.info("CSDN publish dialog required fields handled: summaryFilled={}, originalChecked={}, summaryLength={}, durationMs={}",
                summaryFilled, originalChecked, summary.length(), elapsed(startedAt));
    }

    private PublishResult waitForPublishResult(Page page, PublishContext context, BrowserPublisherConfig config, long startedAt) {
        long deadline = System.currentTimeMillis() + PUBLISH_RESULT_TIMEOUT_MS;
        boolean triedManagePage = false;
        while (System.currentTimeMillis() < deadline) {
            if (isLoginPage(page) || browserAutomationService.looksLoggedOut(page)) {
                log.warn("CSDN publish result login: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
                return PublishResult.needLogin(currentOrConfiguredUrl(page, config), "CSDN 登录态失效，请在打开的浏览器中完成登录后重新执行发布任务");
            }
            if (browserAutomationService.looksCaptchaBlocked(page)) {
                log.warn("CSDN publish result captcha: taskId={}, currentUrl={}", context.taskId(), safeUrl(page));
                return PublishResult.needCaptcha(currentOrConfiguredUrl(page, config), "CSDN 页面需要人工完成验证码或安全验证后重新执行发布任务");
            }
            String currentUrl = safeUrl(page);
            String snapshot = pageSnapshot(page);
            if (looksContentRejected(snapshot)) {
                log.warn("CSDN publish result rejected: taskId={}, durationMs={}, currentUrl={}",
                        context.taskId(), elapsed(startedAt), currentUrl);
                return PublishResult.contentRejected(currentUrl, "CSDN 内容未通过审核或触发平台规则，请在 CSDN 内容管理页查看详情");
            }
            String articleUrl = extractArticleUrl(page, context.title());
            if (isCsdnArticleUrl(articleUrl)) {
                log.info("CSDN publish result success: taskId={}, durationMs={}, publishUrl={}",
                        context.taskId(), elapsed(startedAt), articleUrl);
                return PublishResult.success(articleUrl, "CSDN 发布成功");
            }
            if (isCsdnArticleUrl(currentUrl)) {
                log.info("CSDN publish result success: taskId={}, durationMs={}, publishUrl={}",
                        context.taskId(), elapsed(startedAt), currentUrl);
                return PublishResult.success(currentUrl, "CSDN 发布成功");
            }
            if (looksReviewing(snapshot)) {
                String publishUrl = StringUtils.hasText(articleUrl) ? articleUrl : fallbackManageUrl(config);
                log.info("CSDN publish result success: taskId={}, state=reviewing, durationMs={}, publishUrl={}",
                        context.taskId(), elapsed(startedAt), publishUrl);
                return PublishResult.success(publishUrl, "已提交 CSDN，当前审核中");
            }
            if (looksPublishSucceeded(snapshot)) {
                String publishUrl = StringUtils.hasText(articleUrl) ? articleUrl : fallbackManageUrl(config);
                String message = isCsdnArticleUrl(publishUrl)
                        ? "CSDN 发布成功"
                        : "CSDN 发布成功，但未自动获取正式文章链接，请在内容管理页查看";
                log.info("CSDN publish result success: taskId={}, durationMs={}, publishUrl={}",
                        context.taskId(), elapsed(startedAt), publishUrl);
                return PublishResult.success(publishUrl, message);
            }
            String missing = missingRequiredField(snapshot);
            if (StringUtils.hasText(missing)) {
                log.warn("CSDN publish result failed: taskId={}, reason={}, durationMs={}",
                        context.taskId(), missing, elapsed(startedAt));
                return PublishResult.failed("CSDN 发布失败：" + missing);
            }
            if (!triedManagePage && elapsed(startedAt) > 8_000) {
                triedManagePage = true;
                navigateToManagePage(page, config);
            }
            if (triedManagePage && snapshotContainsTitle(snapshot, context.title())) {
                String publishUrl = isCsdnArticleUrl(articleUrl) ? articleUrl : fallbackManageUrl(config);
                String message = isCsdnArticleUrl(publishUrl)
                        ? "CSDN 发布成功"
                        : "CSDN 发布成功，但未自动获取正式文章链接，请在内容管理页查看";
                log.info("CSDN publish result success: taskId={}, stage=manage-title-match, durationMs={}, publishUrl={}",
                        context.taskId(), elapsed(startedAt), publishUrl);
                return PublishResult.success(publishUrl, message);
            }
            sleep(1_000);
        }
        log.warn("CSDN publish result link fetch failed: taskId={}, durationMs={}, currentUrl={}",
                context.taskId(), elapsed(startedAt), safeUrl(page));
        return PublishResult.linkFetchFailed(fallbackManageUrl(config), "CSDN 发布后未能确认结果，请在 CSDN 内容管理页检查");
    }

    private void logPublishResult(PublishContext context, PublishResult result) {
        if (result.success()) {
            log.info("CSDN publish result success: taskId={}, resultStatus={}", context.taskId(), result.taskStatus());
            return;
        }
        if ("NEED_CAPTCHA".equals(result.taskStatus())) {
            log.warn("CSDN publish result captcha: taskId={}, resultStatus={}", context.taskId(), result.taskStatus());
            return;
        }
        if ("NEED_LOGIN".equals(result.taskStatus())) {
            log.warn("CSDN publish result login: taskId={}, resultStatus={}", context.taskId(), result.taskStatus());
            return;
        }
        if ("CONTENT_REJECTED".equals(result.taskStatus())) {
            log.warn("CSDN publish result rejected: taskId={}, resultStatus={}", context.taskId(), result.taskStatus());
            return;
        }
        if ("LINK_FETCH_FAILED".equals(result.taskStatus())) {
            log.warn("CSDN publish result link fetch failed: taskId={}, resultStatus={}", context.taskId(), result.taskStatus());
            return;
        }
        log.warn("CSDN publish result failed: taskId={}, resultStatus={}", context.taskId(), result.taskStatus());
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
        if (runTitleStrategy(page, normalizedTitle, "hidden-title-input-js-setter", startedAt, deadlineMs,
                () -> setHiddenTitleInputByJs(page, normalizedTitle))) {
            return titleSuccess("hidden-title-input-js-setter", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "direct-input-fill", startedAt, deadlineMs,
                () -> directInputFill(page, normalizedTitle, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS)))) {
            return titleSuccess("direct-input-fill", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "click-active-insert", startedAt, deadlineMs,
                () -> clickDirectInputAndInsert(page, normalizedTitle, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS)))) {
            return titleSuccess("click-active-insert", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "input-click-paste", startedAt, deadlineMs,
                () -> clickDirectInputAndPaste(page, normalizedTitle, nextStrategyDeadline(deadlineMs, TITLE_STRATEGY_BUDGET_MS)))) {
            return titleSuccess("input-click-paste", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "input-js-setter", startedAt, deadlineMs,
                () -> setTitleByJs(page, normalizedTitle))) {
            return titleSuccess("input-js-setter", startedAt);
        }
        if (runTitleStrategy(page, normalizedTitle, "coordinate-active-insert", startedAt, deadlineMs,
                () -> coordinateTitleAndInsert(page, normalizedTitle))) {
            return titleSuccess("coordinate-active-insert", startedAt);
        }
        return titleFailed("all-strategies", startedAt, titleTimedOut(startedAt));
    }

    private boolean runTitleStrategy(Page page, String title, String strategy, long startedAt, long deadlineMs, BooleanSupplier action) {
        if (System.currentTimeMillis() >= deadlineMs) {
            log.warn("CSDN title fill strategy timeout: strategy={}, durationMs={}", strategy, elapsed(startedAt));
            return false;
        }
        log.info("CSDN title fill strategy started: strategy={}, durationMs={}", strategy, elapsed(startedAt));
        boolean attempted;
        try {
            attempted = action.getAsBoolean();
        } catch (RuntimeException exception) {
            log.warn("CSDN title fill strategy failed: strategy={}, durationMs={}, reason={}",
                    strategy, elapsed(startedAt), safeMessage(exception));
            return false;
        }
        if (System.currentTimeMillis() >= deadlineMs) {
            log.warn("CSDN title fill strategy timeout: strategy={}, durationMs={}", strategy, elapsed(startedAt));
            return false;
        }
        if (attempted && titleFilled(page, title)) {
            log.info("CSDN title fill strategy succeeded: strategy={}, durationMs={}", strategy, elapsed(startedAt));
            return true;
        }
        log.warn("CSDN title fill strategy failed: strategy={}, attempted={}, durationMs={}",
                strategy, attempted, elapsed(startedAt));
        return false;
    }

    private boolean setHiddenTitleInputByJs(Page page, String title) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> result = (java.util.Map<String, Object>) page.evaluate("""
                    title => {
                      const selectors = [
                        "input.article-bar__title-input",
                        "input[placeholder*='请输入文章标题']",
                        "input.article-bar__title"
                      ];
                      const findInput = () => {
                        for (const selector of selectors) {
                          const input = document.querySelector(selector);
                          if (input instanceof HTMLInputElement || input instanceof HTMLTextAreaElement) {
                            return input;
                          }
                        }
                        return null;
                      };
                      const dispatchTitleEvents = input => {
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        input.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: 'Process', code: 'Unidentified' }));
                      };
                      const setNativeValue = input => {
                        input.focus();
                        const prototype = input instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
                        const setter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set;
                        if (!setter) return false;
                        setter.call(input, title);
                        dispatchTitleEvents(input);
                        input.blur();
                        return true;
                      };
                      const verify = input => {
                        const bodyText = document.body?.innerText || '';
                        const counterText = Array.from(document.querySelectorAll('body *'))
                          .map(element => element.innerText || element.textContent || '')
                          .find(text => text && text.includes(`/${100}`) && text.includes(String(title.length))) || '';
                        return input.value === title || bodyText.includes(title) || Boolean(counterText);
                      };
                      const input = findInput();
                      if (!input) {
                        return { found: false, valueLength: 0, verified: false };
                      }
                      let setOk = setNativeValue(input);
                      let verified = verify(input);
                      if (!verified) {
                        const previousDisplay = input.style.display;
                        const previousAriaHidden = input.getAttribute('aria-hidden');
                        input.style.display = 'block';
                        input.removeAttribute('aria-hidden');
                        setOk = setNativeValue(input) || setOk;
                        verified = verify(input);
                        input.style.display = previousDisplay;
                        if (previousAriaHidden === null) {
                          input.removeAttribute('aria-hidden');
                        } else {
                          input.setAttribute('aria-hidden', previousAriaHidden);
                        }
                      }
                      return {
                        found: true,
                        setOk,
                        valueLength: (input.value || '').length,
                        verified
                      };
                    }
                    """, title);
            boolean found = Boolean.TRUE.equals(result.get("found"));
            boolean verified = Boolean.TRUE.equals(result.get("verified"));
            Object valueLength = result.get("valueLength");
            log.info("CSDN hidden-title-input-js-setter result: hiddenInputFound={}, valueLength={}, titleVerifyResult={}",
                    found, valueLength, verified);
            return found && (verified || title.length() == numberValue(valueLength));
        } catch (RuntimeException exception) {
            log.warn("CSDN hidden-title-input-js-setter failed: reason={}", safeMessage(exception));
            return false;
        }
    }

    private boolean directInputFill(Page page, String title, long deadlineMs) {
        for (String selector : TITLE_DIRECT_INPUT_SELECTORS) {
            if (System.currentTimeMillis() >= deadlineMs) {
                return false;
            }
            try {
                Locator locator = page.locator(selector).first();
                locator.fill(title, new Locator.FillOptions().setTimeout(boundedTitleTimeout(deadlineMs)));
                return true;
            } catch (RuntimeException ignored) {
                // Try the next direct title input.
            }
        }
        return false;
    }

    private boolean clickDirectInputAndInsert(Page page, String title, long deadlineMs) {
        for (String selector : TITLE_DIRECT_INPUT_SELECTORS) {
            if (System.currentTimeMillis() >= deadlineMs) {
                return false;
            }
            try {
                Locator locator = page.locator(selector).first();
                locator.click(new Locator.ClickOptions().setTimeout(boundedTitleTimeout(deadlineMs)));
                page.keyboard().press(selectAllShortcut());
                page.keyboard().insertText(title);
                return true;
            } catch (RuntimeException ignored) {
                // Try the next direct title input.
            }
        }
        return false;
    }

    private boolean clickDirectInputAndPaste(Page page, String title, long deadlineMs) {
        for (String selector : TITLE_DIRECT_INPUT_SELECTORS) {
            if (System.currentTimeMillis() >= deadlineMs) {
                return false;
            }
            try {
                Locator locator = page.locator(selector).first();
                locator.click(new Locator.ClickOptions().setTimeout(boundedTitleTimeout(deadlineMs)));
                writeClipboard(page, title);
                page.keyboard().press(pasteShortcut());
                return true;
            } catch (RuntimeException ignored) {
                // Try the next direct title input.
            }
        }
        return false;
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
                  const looksTitle = element => /标题|请输入文章标题|无标题|title/i.test(textOf(element));
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
                      element.textContent = value;
                      element.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: value }));
                      element.dispatchEvent(new Event('change', { bubbles: true }));
                      return true;
                    }
                    return false;
                  };
                  const selectors = [
                    "input[placeholder*='请输入文章标题']",
                    "textarea[placeholder*='请输入文章标题']",
                    "input[placeholder*='标题']",
                    "textarea[placeholder*='标题']",
                    "input[name='title']",
                    "[contenteditable='true'][placeholder*='请输入文章标题']",
                    "[contenteditable='true'][aria-label*='标题']",
                    "[role='textbox'][aria-label*='标题']",
                    "#txtTitle"
                  ];
                  for (const selector of selectors) {
                    for (const element of document.querySelectorAll(selector)) {
                      if (visible(element) && looksTitle(element) && setNativeValue(element, title)) return true;
                    }
                  }
                  return false;
                }
                """, title);
        return Boolean.TRUE.equals(filled);
    }

    private boolean coordinateTitleAndInsert(Page page, String title) {
        if (!clickTitleByCoordinate(page)) {
            return false;
        }
        sleep(300);
        if (!activeElementLooksTitle(page)) {
            return false;
        }
        page.keyboard().press(selectAllShortcut());
        page.keyboard().insertText(title);
        return true;
    }

    private boolean clickTitleByCoordinate(Page page) {
        try {
            @SuppressWarnings("unchecked")
            List<Number> point = (List<Number>) page.evaluate("""
                    () => {
                      const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = window.getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                      };
                      const textOf = element => [
                        element.getAttribute?.('placeholder') || '',
                        element.getAttribute?.('aria-label') || '',
                        element.getAttribute?.('title') || '',
                        element.innerText || '',
                        element.textContent || ''
                      ].join(' ').replace(/\\s+/g, ' ').trim();
                      const candidates = Array.from(document.querySelectorAll('input, textarea, [contenteditable="true"], [role="textbox"], span, div, h1, p'))
                        .filter(element => visible(element))
                        .filter(element => /请输入文章标题|无标题|【无标题】|标题/.test(textOf(element)));
                      const element = candidates[0];
                      if (!element) return [];
                      const rect = element.getBoundingClientRect();
                      return [rect.left + rect.width / 2, rect.top + rect.height / 2];
                    }
                    """);
            if (point == null || point.size() < 2) {
                return false;
            }
            page.mouse().click(point.get(0).doubleValue(), point.get(1).doubleValue());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean activeElementLooksTitle(Page page) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> info = (java.util.Map<String, Object>) page.evaluate("""
                    () => {
                      const element = document.activeElement;
                      const text = value => (value || '').toString().replace(/\\s+/g, ' ').trim().slice(0, 80);
                      const tag = element?.tagName || '';
                      const className = typeof element?.className === 'string' ? element.className : '';
                      const placeholder = element?.getAttribute?.('placeholder') || '';
                      const aria = element?.getAttribute?.('aria-label') || '';
                      const role = element?.getAttribute?.('role') || '';
                      const contenteditable = element?.getAttribute?.('contenteditable') || '';
                      const name = element?.getAttribute?.('name') || '';
                      const id = element?.id || '';
                      const innerText = text(element?.innerText || element?.textContent || '');
                      const value = text(element?.value || '');
                      const combined = [tag, className, placeholder, aria, role, contenteditable, name, id, innerText, value].join(' ');
                      const titleLike = /标题|请输入文章标题|title|无标题/i.test(combined);
                      const editable = /INPUT|TEXTAREA/.test(tag) || contenteditable === 'true' || role === 'textbox' || element?.isContentEditable === true;
                      const aiLike = /AI助手|ai-assistant|assistant/i.test(combined)
                        || Boolean(element?.closest?.('[class*="ai"], [id*="ai"], [class*="assistant"], [id*="assistant"]'));
                      return {
                        tag,
                        className: text(className).slice(0, 40),
                        placeholder: text(placeholder),
                        contenteditable,
                        innerText,
                        value,
                        titleLike: Boolean(editable && titleLike && !aiLike)
                      };
                    }
                    """);
            boolean titleLike = Boolean.TRUE.equals(info.get("titleLike"));
            log.info("CSDN title active element checked: tag={}, className={}, placeholder={}, contenteditable={}, titleLike={}, innerText={}, value={}",
                    info.get("tag"), info.get("className"), info.get("placeholder"), info.get("contenteditable"),
                    titleLike, info.get("innerText"), info.get("value"));
            return titleLike;
        } catch (RuntimeException exception) {
            log.warn("CSDN title active element check failed: reason={}", safeMessage(exception));
            return false;
        }
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
        if (!StringUtils.hasText(title)) {
            return true;
        }
        String snapshot = browserAutomationService.textSnapshot(page, titleSelectorsForSnapshot(), 4_000);
        return snapshotContainsContent(snapshot, title);
    }

    private List<String> titleSelectorsForSnapshot() {
        List<String> selectors = new ArrayList<>();
        selectors.addAll(TITLE_VALUE_SELECTORS);
        selectors.add("[class*='title']");
        selectors.add("[id*='title']");
        selectors.add("[placeholder*='标题']");
        return selectors;
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

    private String resolvePublishSummary(PublishContext context, BrowserPublisherConfig config) {
        if (StringUtils.hasText(config.defaultSummary())) {
            return trimToLength(config.defaultSummary(), 180);
        }
        if (StringUtils.hasText(context.summary())) {
            return trimToLength(context.summary(), 180);
        }
        return trimToLength(stripMarkdown(context.content()), 180);
    }

    private boolean checkOriginalStatement(Page page) {
        try {
            Object checked = page.evaluate("""
                    () => {
                      const textOf = element => (element?.innerText || element?.textContent || '').replace(/\\s+/g, ' ').trim();
                      const isOriginalText = text => text && (text.includes('原创声明') || text.includes('原创'));
                      const setChecked = input => {
                        if (!input) return false;
                        if (input.checked) return true;
                        input.click();
                        if (!input.checked) {
                          input.checked = true;
                          input.dispatchEvent(new Event('input', { bubbles: true }));
                          input.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                        return input.checked === true;
                      };
                      const inputs = Array.from(document.querySelectorAll("input[type='checkbox']"));
                      for (const input of inputs) {
                        const id = input.id;
                        const label = id ? document.querySelector(`label[for="${CSS.escape(id)}"]`) : null;
                        const container = input.closest('label, li, div, section');
                        const text = [textOf(label), textOf(container), input.getAttribute('aria-label') || ''].join(' ');
                        if (isOriginalText(text) && setChecked(input)) return true;
                      }
                      const candidates = Array.from(document.querySelectorAll('label, span, div, p'))
                        .filter(element => isOriginalText(textOf(element)));
                      for (const element of candidates) {
                        const container = element.closest('label, li, div, section') || element;
                        const input = container.querySelector("input[type='checkbox']");
                        if (setChecked(input)) return true;
                        try {
                          element.click();
                          const afterClickInput = container.querySelector("input[type='checkbox']");
                          if (!afterClickInput || afterClickInput.checked) return true;
                        } catch (e) {
                        }
                      }
                      return false;
                    }
                    """);
            return Boolean.TRUE.equals(checked);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String pageSnapshot(Page page) {
        return browserAutomationService.textSnapshot(page, List.of(
                "body",
                "a",
                "button",
                "[role='dialog']",
                ".modal",
                ".ant-modal",
                "[class*='dialog']",
                "[class*='modal']",
                "[class*='toast']",
                "[class*='message']"
        ), CONTENT_VERIFY_SNAPSHOT_LENGTH);
    }

    private boolean looksPublishSucceeded(String snapshot) {
        return containsAnyText(snapshot, List.of("发布成功", "已发布", "文章已发布", "提交成功", "内容管理"));
    }

    private boolean looksReviewing(String snapshot) {
        return containsAnyText(snapshot, List.of("审核中", "等待审核", "已提交审核", "提交审核"));
    }

    private boolean looksContentRejected(String snapshot) {
        return containsAnyText(snapshot, List.of("未通过", "内容违规", "审核失败", "审核未通过", "发布失败", "风控", "风险提示"));
    }

    private String missingRequiredField(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return "";
        }
        if (snapshot.contains("缺少分类") || snapshot.contains("请选择分类") || snapshot.contains("分类必填")) {
            return "缺少分类";
        }
        if (snapshot.contains("缺少标签") || snapshot.contains("请选择标签") || snapshot.contains("标签必填")) {
            return "缺少标签";
        }
        if (snapshot.contains("摘要必填") || snapshot.contains("请输入摘要") || snapshot.contains("缺少摘要")) {
            return "摘要必填";
        }
        if (snapshot.contains("必填") || snapshot.contains("不能为空") || snapshot.contains("请选择")) {
            return "存在必填项未填写";
        }
        return "";
    }

    private boolean containsAnyText(String snapshot, List<String> needles) {
        if (!StringUtils.hasText(snapshot)) {
            return false;
        }
        return needles.stream().anyMatch(snapshot::contains);
    }

    private boolean snapshotContainsTitle(String snapshot, String title) {
        if (!StringUtils.hasText(title)) {
            return false;
        }
        return snapshotContainsContent(snapshot, title);
    }

    private String extractArticleUrl(Page page, String title) {
        try {
            Object value = page.evaluate("""
                    title => {
                      const normalize = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const titleText = normalize(title);
                      const links = Array.from(document.querySelectorAll('a[href]'));
                      const candidates = links
                        .map(link => ({
                          href: link.href || '',
                          text: normalize(link.innerText || link.textContent || '')
                        }))
                        .filter(item => item.href.includes('csdn.net') && (
                          item.href.includes('/article/details/')
                          || item.href.includes('blog.csdn.net')
                        ));
                      const exact = candidates.find(item => titleText && item.text.includes(titleText));
                      return (exact || candidates[0] || {}).href || '';
                    }
                    """, title);
            return value instanceof String text ? text : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private boolean isCsdnArticleUrl(String url) {
        return StringUtils.hasText(url)
                && url.contains("csdn.net")
                && (url.contains("/article/details/") || url.contains("blog.csdn.net"));
    }

    private void navigateToManagePage(Page page, BrowserPublisherConfig config) {
        try {
            page.navigate(fallbackManageUrl(config), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(10_000));
        } catch (RuntimeException ignored) {
            // Result detection will continue with the current page.
        }
    }

    private String fallbackManageUrl(BrowserPublisherConfig config) {
        return StringUtils.hasText(config.manageUrl()) ? config.manageUrl() : DEFAULT_MANAGE_URL;
    }

    private void scrollToBottom(Page page) {
        try {
            page.evaluate("() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'instant' })");
        } catch (RuntimeException ignored) {
            // Best effort only.
        }
    }

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String stripMarkdown(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replaceAll("```[\\s\\S]*?```", " ")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("[#>*_~`\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
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

    private int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private double boundedTitleTimeout(long deadlineMs) {
        long remainingMs = deadlineMs - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 1;
        }
        return Math.max(1, Math.min(TITLE_STRATEGY_TIMEOUT_MS, remainingMs));
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

    private String selectAllShortcut() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac") ? "Meta+A" : "Control+A";
    }

    private String pasteShortcut() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac") ? "Meta+V" : "Control+V";
    }

    private enum EditorType {
        MARKDOWN,
        RICH_TEXT,
        UNKNOWN
    }

    private record FillOutcome(boolean success, boolean timedOut, String strategy) {
    }
}
