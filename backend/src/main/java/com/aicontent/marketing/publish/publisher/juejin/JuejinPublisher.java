package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.publish.publisher.PlatformPublisher;
import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JuejinPublisher implements PlatformPublisher {

    private static final String PLATFORM = "JUEJIN";
    private static final String MODE = "UNOFFICIAL_API";

    private final JuejinClient juejinClient;
    private final ObjectMapper objectMapper;

    public JuejinPublisher(JuejinClient juejinClient, ObjectMapper objectMapper) {
        this.juejinClient = juejinClient;
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
            JuejinAuthConfig config = JuejinAuthConfig.parse(context.accountAuthConfig(), objectMapper);
            JuejinClient.JuejinDraftUpdateResult updateResult = juejinClient.updateDraft(
                    config,
                    new JuejinClient.JuejinDraftUpdateRequest(
                            normalize(context.title(), "未命名掘金草稿"),
                            normalize(context.summary(), ""),
                            normalize(context.content(), "")
                    )
            );

            if (!config.draftOnly()) {
                juejinClient.publishArticle(config);
            }

            String draftUrl = "https://juejin.cn/editor/drafts/" + updateResult.draftId();
            String message = config.draftOnly() ? "掘金草稿更新成功" : "掘金草稿更新并提交发布成功";
            return PublishResult.success(draftUrl, message);
        } catch (BusinessException exception) {
            return PublishResult.failed(exception.getMessage());
        } catch (Exception exception) {
            return PublishResult.failed("掘金发布失败：" + safeMessage(exception));
        }
    }

    private void validateContext(PublishContext context) {
        if (!PLATFORM.equals(context.platform())) {
            throw new BusinessException("JuejinPublisher 只支持 JUEJIN 平台");
        }
        if (!MODE.equals(context.publishMode())) {
            throw new BusinessException("JuejinPublisher 只支持 UNOFFICIAL_API 发布方式");
        }
        if (!StringUtils.hasText(context.content())) {
            throw new BusinessException("掘金平台稿正文不能为空");
        }
        if (!context.accountConfigExists() || !StringUtils.hasText(context.accountAuthConfig())) {
            throw new BusinessException("掘金平台账号 auth_config 未配置");
        }
    }

    private String normalize(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String safeMessage(Exception exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
