package com.aicontent.marketing.publish.publisher.browser;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record BrowserPublisherConfig(
        String browserUserDataDir,
        String editorUrl,
        List<String> defaultTags,
        List<String> defaultTopics,
        String defaultCategory,
        String defaultColumn,
        String defaultSummary,
        String manageUrl,
        boolean headless,
        double timeoutMs,
        double waitAfterFillMs
) {

    public static BrowserPublisherConfig parse(
            String rawConfig,
            ObjectMapper objectMapper,
            String platform,
            String defaultEditorUrl,
            String defaultManageUrl,
            String allowedProfileRoot
    ) {
        if (!StringUtils.hasText(rawConfig)) {
            throw new BusinessException("浏览器自动化 auth_config 未配置");
        }
        try {
            JsonNode root = objectMapper.readTree(rawConfig);
            BrowserPublisherConfig config = new BrowserPublisherConfig(
                    text(root, "browserUserDataDir", null),
                    text(root, "editorUrl", defaultEditorUrl),
                    stringList(root.path("defaultTags")),
                    stringList(root.path("defaultTopics")),
                    text(root, "defaultCategory", ""),
                    text(root, "defaultColumn", ""),
                    text(root, "defaultSummary", ""),
                    text(root, "manageUrl", defaultManageUrl),
                    bool(root, "headless", false),
                    number(root, "timeoutMs", 30_000),
                    number(root, "waitAfterFillMs", 1_000)
            );
            config.validate(platform, allowedProfileRoot);
            return config;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("浏览器自动化 auth_config JSON 解析失败，请检查认证配置格式");
        }
    }

    private void validate(String platform, String allowedProfileRoot) {
        if (!StringUtils.hasText(browserUserDataDir)) {
            throw new BusinessException("browserUserDataDir 未配置，请在平台账号 auth_config 中填写浏览器用户目录");
        }
        if (!StringUtils.hasText(editorUrl)) {
            throw new BusinessException("editorUrl 未配置，请在平台账号 auth_config 中填写编辑器地址");
        }
        validatePlatformUrl(editorUrl, platform, "editorUrl");
        if (StringUtils.hasText(manageUrl)) {
            validatePlatformUrl(manageUrl, platform, "manageUrl");
        }
        validateProfileRoot(allowedProfileRoot);
    }

    private void validatePlatformUrl(String rawUrl, String platform, String fieldName) {
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            String allowedDomain = switch (platform) {
                case "CSDN" -> "csdn.net";
                case "ZHIHU" -> "zhihu.com";
                default -> throw new BusinessException("不支持的浏览器发布平台");
            };
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !StringUtils.hasText(host)
                    || !(host.equalsIgnoreCase(allowedDomain) || host.toLowerCase().endsWith("." + allowedDomain))) {
                throw new BusinessException(fieldName + " 必须使用对应平台的 HTTPS 域名");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(fieldName + " 格式无效");
        }
    }

    private void validateProfileRoot(String allowedProfileRoot) {
        if (!StringUtils.hasText(allowedProfileRoot)) {
            return;
        }
        try {
            Path configured = Path.of(browserUserDataDir).toAbsolutePath().normalize();
            Path allowed = Path.of(allowedProfileRoot).toAbsolutePath().normalize();
            if (!configured.startsWith(allowed)) {
                throw new BusinessException("browserUserDataDir 必须位于允许的浏览器用户目录下");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("browserUserDataDir 格式无效");
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

    private static double number(JsonNode root, String field, double defaultValue) {
        JsonNode value = root.path(field);
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Double.parseDouble(value.asText());
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
