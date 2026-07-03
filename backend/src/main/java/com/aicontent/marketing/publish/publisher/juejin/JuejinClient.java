package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final String BASE_URL = "https://api.juejin.cn/content_api/v1";
    private static final String JUEJIN_ORIGIN = "https://juejin.cn";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public JuejinClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    public JuejinDraftUpdateResult updateDraft(JuejinAuthConfig config, JuejinDraftUpdateRequest updateRequest) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", config.draftId());
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

            HttpResponse<String> response = send(config, "/article_draft/update", requestBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("掘金草稿更新失败，HTTP 状态码：" + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            int errNo = root.path("err_no").asInt(-1);
            if (errNo != 0) {
                String errMsg = root.path("err_msg").asText("unknown error");
                throw new BusinessException("掘金草稿更新失败：" + errMsg);
            }

            String draftId = root.path("data").path("id").asText(config.draftId());
            if (!StringUtils.hasText(draftId)) {
                draftId = config.draftId();
            }
            return new JuejinDraftUpdateResult(draftId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金草稿更新失败：" + safeMessage(exception));
        }
    }

    public void publishArticle(JuejinAuthConfig config) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "draft_id", config.draftId(),
                    "sync_to_org", config.syncToOrg(),
                    "column_ids", config.columnIds(),
                    "theme_ids", config.themeIds(),
                    "encrypted_word_count", config.encryptedWordCount(),
                    "origin_word_count", config.originWordCount()
            ));

            HttpResponse<String> response = send(config, "/article/publish", requestBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("掘金正式发布失败，HTTP 状态码：" + response.statusCode());
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("掘金正式发布失败：" + safeMessage(exception));
        }
    }

    private HttpResponse<String> send(JuejinAuthConfig config, String path, String requestBody) throws Exception {
        String url = BASE_URL + path + "?aid=" + encode(config.aid()) + "&uuid=" + encode(config.uuid());
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

    public record JuejinDraftUpdateRequest(
            String title,
            String summary,
            String content
    ) {
    }

    public record JuejinDraftUpdateResult(String draftId) {
    }
}
