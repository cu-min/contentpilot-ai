package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

public record WechatAuthConfig(
        String appId,
        String appSecret,
        String defaultThumbMediaId,
        String author,
        boolean draftOnly,
        int needOpenComment,
        int onlyFansCanComment,
        String sourceUrl
) {

    public static WechatAuthConfig parse(String rawConfig, ObjectMapper objectMapper) {
        return parse(rawConfig, objectMapper, true);
    }

    public static WechatAuthConfig parseForDefaultCoverUpload(String rawConfig, ObjectMapper objectMapper) {
        return parse(rawConfig, objectMapper, false);
    }

    private static WechatAuthConfig parse(String rawConfig, ObjectMapper objectMapper, boolean requireDefaultThumbMediaId) {
        if (!StringUtils.hasText(rawConfig)) {
            throw new BusinessException("微信公众号认证配置未配置");
        }
        try {
            JsonNode root = objectMapper.readTree(rawConfig);
            WechatAuthConfig config = new WechatAuthConfig(
                    text(root, "appId", null),
                    text(root, "appSecret", null),
                    text(root, "defaultThumbMediaId", null),
                    text(root, "author", ""),
                    bool(root, "draftOnly", true),
                    integer(root, "needOpenComment", 0),
                    integer(root, "onlyFansCanComment", 0),
                    text(root, "sourceUrl", "")
            );
            config.validate(requireDefaultThumbMediaId);
            return config;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("微信公众号认证配置格式错误");
        }
    }

    private void validate(boolean requireDefaultThumbMediaId) {
        if (!StringUtils.hasText(appId)) {
            throw new BusinessException("微信公众号 AppID 未配置");
        }
        if (!StringUtils.hasText(appSecret)) {
            throw new BusinessException("微信公众号 AppSecret 未配置");
        }
        if (requireDefaultThumbMediaId && !StringUtils.hasText(defaultThumbMediaId)) {
            throw new BusinessException("微信公众号默认封面素材 media_id 未配置");
        }
    }

    private static String text(JsonNode root, String field, String defaultValue) {
        JsonNode value = root.path(field);
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            return value.asText();
        }
        return defaultValue;
    }

    private static boolean bool(JsonNode root, String field, boolean defaultValue) {
        JsonNode value = root.path(field);
        return value.isBoolean() ? value.asBoolean() : defaultValue;
    }

    private static int integer(JsonNode root, String field, int defaultValue) {
        JsonNode value = root.path(field);
        if (value.canConvertToInt()) {
            return value.asInt();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
