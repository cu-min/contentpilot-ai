package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public record JuejinAuthConfig(
        String cookie,
        String userAgent,
        String csrfToken,
        String aid,
        String uuid,
        String draftId,
        String defaultCategoryId,
        List<String> defaultTagIds,
        boolean draftOnly,
        boolean syncToOrg,
        List<String> columnIds,
        List<String> themeIds,
        int encryptedWordCount,
        int originWordCount
) {
    private static final String DEFAULT_AID = "2608";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0";
    private static final int DEFAULT_ENCRYPTED_WORD_COUNT = 1077885;
    private static final int DEFAULT_ORIGIN_WORD_COUNT = 1;

    public static JuejinAuthConfig parse(String rawConfig, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(rawConfig)) {
            throw new BusinessException("掘金 auth_config 未配置");
        }
        try {
            JsonNode root = objectMapper.readTree(rawConfig);
            JuejinAuthConfig config = new JuejinAuthConfig(
                    text(root, "cookie", null),
                    text(root, "userAgent", DEFAULT_USER_AGENT),
                    text(root, "csrfToken", ""),
                    text(root, "aid", DEFAULT_AID),
                    text(root, "uuid", ""),
                    text(root, "draftId", null),
                    text(root, "defaultCategoryId", null),
                    stringList(root.path("defaultTagIds")),
                    bool(root, "draftOnly", true),
                    bool(root, "syncToOrg", false),
                    stringList(root.path("columnIds")),
                    stringList(root.path("themeIds")),
                    integer(root, "encryptedWordCount", DEFAULT_ENCRYPTED_WORD_COUNT),
                    integer(root, "originWordCount", DEFAULT_ORIGIN_WORD_COUNT)
            );
            config.validate();
            return config;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金 auth_config JSON 解析失败，请检查认证配置格式");
        }
    }

    private void validate() {
        if (!StringUtils.hasText(cookie)) {
            throw new BusinessException("掘金 Cookie 未配置，请在平台账号认证配置中填写 cookie");
        }
        if (!StringUtils.hasText(draftId)) {
            throw new BusinessException("掘金 draftId 未配置，请从 /editor/drafts/{draftId} 地址中获取");
        }
        if (!StringUtils.hasText(defaultCategoryId)) {
            throw new BusinessException("掘金默认分类 ID 未配置，请从 article_draft/update 请求 Payload 的 category_id 获取");
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

    private static List<String> stringList(JsonNode value) {
        if (!value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> {
            if (item.isTextual() && StringUtils.hasText(item.asText())) {
                result.add(item.asText());
            }
        });
        return result;
    }
}
