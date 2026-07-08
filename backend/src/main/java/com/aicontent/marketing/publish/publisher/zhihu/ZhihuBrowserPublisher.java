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
import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ZhihuBrowserPublisher implements PlatformPublisher {

    private static final String PLATFORM = "ZHIHU";
    private static final String MODE_BROWSER_AUTOMATION = "BROWSER_AUTOMATION";
    private static final String MODE_MANUAL_CONFIRM = "MANUAL_CONFIRM";
    private static final String DEFAULT_EDITOR_URL = "https://zhuanlan.zhihu.com/write";

    private final BrowserAutomationService browserAutomationService;
    private final ObjectMapper objectMapper;

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
                return PublishResult.needCaptcha(config.editorUrl(), "知乎页面需要人工完成验证码后重新执行发布任务");
            }
            if (browserAutomationService.looksLoggedOut(page)) {
                return PublishResult.needLogin(config.editorUrl(), "知乎未检测到登录态，请在打开的浏览器中完成登录后重新执行发布任务");
            }
            // TODO: 后续补充知乎标题、正文、话题等编辑器字段的稳定填充逻辑。
            return PublishResult.needManualConfirm(
                    config.editorUrl(),
                    config.editorUrl(),
                    "知乎写文章页面已打开，请人工确认发布；正文自动填充待后续实现"
            );
        } catch (BusinessException exception) {
            return PublishResult.failed(exception.getMessage());
        } catch (RuntimeException exception) {
            return PublishResult.linkFetchFailed(null, "知乎写文章页面打开失败：" + safeMessage(exception));
        }
    }

    private void validateContext(PublishContext context) {
        if (!PLATFORM.equals(context.platform())) {
            throw new BusinessException("ZhihuBrowserPublisher 只支持 ZHIHU 平台");
        }
        if (!MODE_BROWSER_AUTOMATION.equals(context.publishMode()) && !MODE_MANUAL_CONFIRM.equals(context.publishMode())) {
            throw new BusinessException("ZhihuBrowserPublisher 只支持 BROWSER_AUTOMATION 或 MANUAL_CONFIRM 发布方式");
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
}
