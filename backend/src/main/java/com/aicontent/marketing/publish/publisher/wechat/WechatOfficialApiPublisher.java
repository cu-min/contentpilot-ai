package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class WechatOfficialApiPublisher implements PlatformPublisher {

    private static final String PLATFORM = "WECHAT_OFFICIAL";
    private static final String MODE = "OFFICIAL_API";

    private final WechatClient wechatClient;
    private final WechatAccessTokenCache accessTokenCache;
    private final ObjectMapper objectMapper;

    public WechatOfficialApiPublisher(
            WechatClient wechatClient,
            WechatAccessTokenCache accessTokenCache,
            ObjectMapper objectMapper
    ) {
        this.wechatClient = wechatClient;
        this.accessTokenCache = accessTokenCache;
        this.objectMapper = objectMapper;
    }

    @Override
    public String platform() {
        return PLATFORM;
    }

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public PublishResult publish(PublishContext context) {
        try {
            validateContext(context);
            WechatAuthConfig config = WechatAuthConfig.parse(context.accountAuthConfig(), objectMapper);
            String accessToken = accessTokenCache.getToken(config.appId(), config.appSecret(), wechatClient);
            String htmlContent = convertMarkdownToHtml(context.content());
            WechatDraftAddResponse response = wechatClient.createDraft(accessToken, new WechatDraftAddRequest(List.of(
                    new WechatDraftAddRequest.Article(
                            normalize(context.title(), "未命名公众号草稿"),
                            normalize(config.author(), ""),
                            normalize(context.summary(), ""),
                            htmlContent,
                            config.defaultThumbMediaId(),
                            normalize(config.sourceUrl(), ""),
                            config.needOpenComment(),
                            config.onlyFansCanComment()
                    )
            )));
            String mediaId = response.mediaId();
            return PublishResult.prepared(
                    mediaId,
                    "wechat-draft:" + mediaId,
                    "微信公众号草稿已创建，请在公众号后台检查后人工发布"
            );
        } catch (BusinessException exception) {
            return PublishResult.failed(exception.getMessage());
        } catch (Exception exception) {
            return PublishResult.failed("创建微信公众号草稿失败：" + safeMessage(exception));
        }
    }

    private void validateContext(PublishContext context) {
        if (!PLATFORM.equals(context.platform())) {
            throw new BusinessException("WechatOfficialApiPublisher 只支持 WECHAT_OFFICIAL 平台");
        }
        if (!MODE.equals(context.publishMode())) {
            throw new BusinessException("WechatOfficialApiPublisher 只支持 OFFICIAL_API 发布方式");
        }
        if (!context.accountConfigExists() || !StringUtils.hasText(context.accountAuthConfig())) {
            throw new BusinessException("微信公众号认证配置未配置");
        }
    }

    private String convertMarkdownToHtml(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            throw new BusinessException("公众号正文内容不能为空");
        }
        Node document = Parser.builder().build().parse(markdown);
        return HtmlRenderer.builder().build().render(document);
    }

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String safeMessage(Exception exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
