package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JuejinClient {

    private static final Logger log = LoggerFactory.getLogger(JuejinClient.class);
    private static final String DEFAULT_BASE_URL = "https://api.juejin.cn/content_api/v1";
    private static final String JUEJIN_ORIGIN = "https://juejin.cn";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;

    @Autowired
    public JuejinClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build(), DEFAULT_BASE_URL);
    }

    JuejinClient(ObjectMapper objectMapper, HttpClient httpClient, String baseUrl) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    public JuejinDraftCreateResult createDraft(JuejinAuthConfig config) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("category_id", "0");
            payload.put("tag_ids", List.of());
            payload.put("link_url", "");
            payload.put("cover_image", "");
            payload.put("title", "");
            payload.put("brief_content", "");
            payload.put("edit_type", 10);
            payload.put("html_content", "deprecated");
            payload.put("mark_content", "init");
            payload.put("theme_ids", List.of());
            payload.put("pics", List.of());
            String requestBody = objectMapper.writeValueAsString(payload);

            JsonNode root = sendAndRead(config, "/article_draft/create", requestBody, "掘金草稿创建失败");
            String draftId = root.path("data").path("id").asText();
            if (!StringUtils.hasText(draftId)) {
                throw new BusinessException("掘金草稿创建失败：响应中缺少草稿 ID");
            }
            return new JuejinDraftCreateResult(draftId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金草稿创建失败：" + safeMessage(exception));
        }
    }

    public JuejinDraftUpdateResult updateDraft(
            JuejinAuthConfig config,
            String draftId,
            JuejinDraftUpdateRequest updateRequest
    ) {
        try {
            if (!StringUtils.hasText(draftId)) {
                throw new BusinessException("掘金草稿 ID 不能为空");
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", draftId);
            payload.put("category_id", config.defaultCategoryId());
            payload.put("tag_ids", config.defaultTagIds());
            payload.put("link_url", "");
            payload.put("cover_image", "");
            payload.put("title", updateRequest.title());
            payload.put("brief_content", updateRequest.summary());
            payload.put("mark_content", updateRequest.content());
            payload.put("html_content", "deprecated");
            payload.put("edit_type", 10);
            payload.put("is_original", 1);
            payload.put("is_gfw", 0);
            payload.put("is_english", 0);
            payload.put("pics", List.of());
            payload.put("theme_ids", config.themeIds());
            String requestBody = objectMapper.writeValueAsString(payload);

            JsonNode root = sendAndRead(config, "/article_draft/update", requestBody, "掘金草稿更新失败");
            String responseDraftId = root.path("data").path("id").asText(draftId);
            if (!StringUtils.hasText(responseDraftId)) {
                responseDraftId = draftId;
            }
            return new JuejinDraftUpdateResult(responseDraftId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金草稿更新失败：" + safeMessage(exception));
        }
    }

    public JuejinArticleStatusResult queryArticleStatus(JuejinAuthConfig config, String articleId) {
        try {
            if (!StringUtils.hasText(articleId)) {
                throw new BusinessException("掘金文章 ID 不能为空");
            }
            JsonNode info = queryArticleInfo(config, articleId);
            if (!StringUtils.hasText(info.path("article_id").asText())) {
                throw new BusinessException("掘金文章状态刷新失败：响应中缺少文章信息");
            }
            return new JuejinArticleStatusResult(info.path("article_id").asText(), resolveArticleStatus(info));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金文章状态刷新失败：" + safeMessage(exception));
        }
    }

    public JuejinArticleStatusResult queryArticleStatusByDraftId(JuejinAuthConfig config, String draftId) {
        try {
            if (!StringUtils.hasText(draftId)) {
                throw new BusinessException("掘金草稿 ID 不能为空");
            }
            JsonNode info = queryArticleInfoByDraftId(config, draftId);
            String articleId = info.path("article_id").asText();
            if (!StringUtils.hasText(articleId) || "0".equals(articleId)) {
                throw new BusinessException("掘金文章状态刷新失败：草稿未返回正式文章 ID");
            }
            return new JuejinArticleStatusResult(articleId, resolveArticleStatus(info));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金文章状态刷新失败：" + safeMessage(exception));
        }
    }

    private JsonNode sendAndRead(
            JuejinAuthConfig config,
            String path,
            String requestBody,
            String failurePrefix
    ) throws Exception {
        HttpResponse<String> response = send(config, path, requestBody);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(failurePrefix + "，HTTP 状态码：" + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        int errNo = root.path("err_no").asInt(-1);
        if (errNo != 0) {
            String errMsg = root.path("err_msg").asText("unknown error");
            throw new BusinessException(failurePrefix + "：" + errMsg);
        }
        return root;
    }

    private HttpResponse<String> send(JuejinAuthConfig config, String path, String requestBody) throws Exception {
        String url = baseUrl + path + "?aid=" + encode(config.aid()) + "&uuid=" + encode(config.uuid());
        if ("/article/detail".equals(path)) {
            url += "&spider=0";
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.COOKIE, config.cookie())
                .header(HttpHeaders.USER_AGENT, config.userAgent())
                .header(HttpHeaders.ORIGIN, JUEJIN_ORIGIN)
                .header(HttpHeaders.REFERER, JUEJIN_ORIGIN + "/")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (StringUtils.hasText(config.csrfToken())) {
            builder.header("x-secsdk-csrf-token", config.csrfToken());
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        log.info("Juejin request completed: path={}, status={}", path, response.statusCode());
        return response;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safeMessage(Exception exception) {
        return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
    }

    private String readArticleId(JsonNode root) {
        String articleId = root.path("data").path("article_id").asText();
        if (StringUtils.hasText(articleId)) {
            return articleId;
        }
        articleId = root.path("data").path("article_info").path("article_id").asText();
        return StringUtils.hasText(articleId) ? articleId : "";
    }

    private JsonNode queryArticleInfo(JuejinAuthConfig config, String articleId) throws Exception {
        JsonNode root = sendAndRead(
                config,
                "/article/detail",
                objectMapper.writeValueAsString(Map.of("article_id", articleId)),
                "掘金文章状态刷新失败"
        );
        return readArticleInfo(root);
    }

    private JsonNode queryArticleInfoByDraftId(JuejinAuthConfig config, String draftId) throws Exception {
        BusinessException firstException = null;
        try {
            JsonNode root = sendAndRead(
                    config,
                    "/article/detail",
                    objectMapper.writeValueAsString(Map.of("draft_id", draftId)),
                    "掘金文章状态刷新失败"
            );
            JsonNode info = readArticleInfo(root);
            if (draftMatches(info, draftId)) {
                return info;
            }
        } catch (BusinessException exception) {
            firstException = exception;
        }

        try {
            JsonNode root = sendAndRead(
                    config,
                    "/article_draft/detail",
                    objectMapper.writeValueAsString(Map.of("draft_id", draftId)),
                    "掘金文章状态刷新失败"
            );
            JsonNode info = readArticleInfo(root);
            if (StringUtils.hasText(info.path("article_id").asText())) {
                return info;
            }
        } catch (BusinessException exception) {
            if (firstException != null) {
                throw firstException;
            }
            throw exception;
        }

        throw new BusinessException("掘金文章状态刷新失败：未能通过草稿 ID 找到正式文章");
    }

    private JsonNode readArticleInfo(JsonNode root) {
        JsonNode info = root.path("data").path("article_info");
        if (info.isMissingNode() || info.isNull()) {
            info = root.path("data");
        }
        return info;
    }

    private boolean draftMatches(JsonNode info, String draftId) {
        return draftId.equals(info.path("draft_id").asText());
    }

    private String resolveArticleStatus(JsonNode info) {
        String statusText = (
                info.path("status").asText("") + " " +
                        info.path("audit_status").asText("") + " " +
                        info.path("review_status").asText("") + " " +
                        info.path("article_status").asText("") + " " +
                        info.path("status_text").asText("") + " " +
                        info.path("audit_msg").asText("")
        ).toLowerCase();
        if (statusText.contains("reject") || statusText.contains("fail")
                || statusText.contains("未通过") || statusText.contains("驳回")) {
            return "REJECTED";
        }
        if (statusText.contains("publish") || statusText.contains("published")
                || statusText.contains("已发布") || statusText.contains("通过")) {
            return "PUBLISHED";
        }
        if (readLong(info.path("rtime").asText("")) > 0) {
            return "PUBLISHED";
        }
        if (statusText.contains("audit") || statusText.contains("review")
                || statusText.contains("审核")) {
            return "REVIEWING";
        }
        return "REVIEWING";
    }

    private long readLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 0;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public record JuejinDraftCreateResult(String draftId) {
    }

    public record JuejinDraftUpdateRequest(
            String title,
            String summary,
            String content
    ) {
    }

    public record JuejinDraftUpdateResult(String draftId) {
    }

    public record JuejinArticleStatusResult(String articleId, String articleStatus) {
    }
}
