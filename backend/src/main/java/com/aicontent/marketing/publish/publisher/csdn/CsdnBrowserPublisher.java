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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class CsdnBrowserPublisher implements PlatformPublisher {

    private static final String PLATFORM = "CSDN";
    private static final String MODE_BROWSER_AUTOMATION = "BROWSER_AUTOMATION";
    private static final String MODE_MANUAL_CONFIRM = "MANUAL_CONFIRM";
    private static final String DEFAULT_EDITOR_URL = "https://editor.csdn.net/md/";
    private static final String MANUAL_CONFIRM_MESSAGE = "已自动填充 CSDN 编辑器，请在浏览器中人工确认并发布。";

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
            Page page = session.page();
            if (browserAutomationService.looksCaptchaBlocked(page)) {
                return PublishResult.needCaptcha(config.editorUrl(), "CSDN 页面需要人工完成验证码或安全验证后重新执行发布任务");
            }
            if (browserAutomationService.looksLoggedOut(page)) {
                return PublishResult.needLogin(config.editorUrl(), "CSDN 未检测到登录态，请在打开的浏览器中完成登录后重新执行发布任务");
            }

            if (!fillTitle(page, context.title(), config.timeoutMs())) {
                return PublishResult.failed("CSDN 编辑器标题输入框未找到，自动填充失败，请人工检查页面结构");
            }
            if (!fillContent(page, context.content(), config.timeoutMs())) {
                return PublishResult.failed("CSDN 编辑器正文区域未找到或不可写，自动填充失败，请人工检查页面结构");
            }
            fillOptionalMetadata(page, context, config);
            return PublishResult.needManualConfirm(
                    config.editorUrl(),
                    config.editorUrl(),
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
        return browserAutomationService.fillFirstInPageOrFrames(page, List.of(
                "input[placeholder*='请输入文章标题']",
                "textarea[placeholder*='请输入文章标题']",
                "input[placeholder*='标题']",
                "textarea[placeholder*='标题']",
                "input[name='title']",
                "[aria-label*='标题']",
                "#txtTitle"
        ), normalize(title, "未命名 CSDN 草稿"), timeoutMs);
    }

    private boolean fillContent(Page page, String content, double timeoutMs) {
        String normalizedContent = normalize(content, "");
        List<String> fillSelectors = List.of(
                "textarea[placeholder*='正文']",
                "textarea[placeholder*='开始创作']",
                "textarea[placeholder*='请输入正文']",
                "textarea.markdown-editor",
                "textarea#md-editor",
                ".bytemd-editor textarea",
                ".editor textarea"
        );
        if (browserAutomationService.fillFirstInPageOrFrames(page, fillSelectors, normalizedContent, timeoutMs)) {
            return true;
        }
        List<String> editorSelectors = List.of(
                "[role='textbox']",
                ".cm-content",
                ".CodeMirror-code",
                "[contenteditable='true']",
                ".monaco-editor textarea"
        );
        if (browserAutomationService.clickAndInsertFirstInPageOrFrames(page, editorSelectors, normalizedContent, timeoutMs)) {
            return true;
        }
        return browserAutomationService.pasteFirstInPageOrFrames(page, editorSelectors, normalizedContent, timeoutMs);
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

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String safeMessage(RuntimeException exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
