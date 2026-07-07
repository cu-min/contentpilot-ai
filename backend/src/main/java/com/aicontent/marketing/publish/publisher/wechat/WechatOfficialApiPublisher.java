package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.service.markdown.MarkdownToWechatHtmlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class WechatOfficialApiPublisher implements PlatformPublisher {

    private static final String PLATFORM = "WECHAT_OFFICIAL";
    private static final String MODE = "OFFICIAL_API";

    private final WechatClient wechatClient;
    private final WechatAccessTokenCache accessTokenCache;
    private final MarkdownToWechatHtmlService markdownToWechatHtmlService;
    private final ObjectMapper objectMapper;

    public WechatOfficialApiPublisher(
            WechatClient wechatClient,
            WechatAccessTokenCache accessTokenCache,
            MarkdownToWechatHtmlService markdownToWechatHtmlService,
            ObjectMapper objectMapper
    ) {
        this.wechatClient = wechatClient;
        this.accessTokenCache = accessTokenCache;
        this.markdownToWechatHtmlService = markdownToWechatHtmlService;
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
            String htmlContent = markdownToWechatHtmlService.convert(context.content());
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
            if (!config.draftOnly()) {
                WechatFreePublishSubmitResponse submitResponse = wechatClient.submitFreePublish(accessToken, mediaId);
                return PublishResult.submitted(
                        "wechat-publish:" + submitResponse.publishId(),
                        mediaId,
                        submitResponse.publishId(),
                        null,
                        "已提交微信发布，等待平台确认"
                );
            }
            return PublishResult.success(
                    "wechat-draft:" + mediaId,
                    mediaId,
                    null,
                    null,
                    "微信公众号草稿创建成功"
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

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String safeMessage(Exception exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
