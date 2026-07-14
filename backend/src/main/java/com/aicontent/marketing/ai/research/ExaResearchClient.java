package com.aicontent.marketing.ai.research;

import com.aicontent.marketing.ai.config.ExaProperties;
import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExaResearchClient {

    private final ExaProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ExaResearchClient(ExaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    public List<ResearchSourceDraft> search(ResearchPlan plan) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException("Exa API Key 未配置");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", plan.getQuery());
            body.put("type", "auto");
            body.put("numResults", 10);
            body.put("contents", Map.of("highlights", true));
            if (plan.getStartPublishedDate() != null) {
                body.put("startPublishedDate", plan.getStartPublishedDate().toString());
            }
            if (plan.getEndPublishedDate() != null) {
                body.put("endPublishedDate", plan.getEndPublishedDate().toString());
            }
            HttpResponse<String> response = send("/search", body);
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new BusinessException("Exa API Key 无效或没有权限");
            }
            if (response.statusCode() == 429) {
                throw new BusinessException("Exa 请求过于频繁，请稍后重试");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Exa 联网检索失败，请稍后重试");
            }
            return parseResults(objectMapper.readTree(response.body()));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("Exa 联网检索失败，请检查网络或稍后重试");
        }
    }

    public ResearchSourceDraft fetchOfficialSource(String officialUrl) {
        if (!StringUtils.hasText(officialUrl) || !StringUtils.hasText(properties.getApiKey())) {
            return null;
        }
        try {
            HttpResponse<String> response = send("/contents", Map.of(
                    "urls", List.of(officialUrl),
                    "highlights", true
            ));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            JsonNode results = objectMapper.readTree(response.body()).path("results");
            if (!results.isArray() || results.isEmpty()) {
                return null;
            }
            return toSource(results.get(0), "PRODUCT_OFFICIAL", officialUrl);
        } catch (Exception exception) {
            return null;
        }
    }

    private HttpResponse<String> send(String path, Map<String, ?> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl() + path))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("x-api-key", properties.getApiKey())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private List<ResearchSourceDraft> parseResults(JsonNode root) {
        List<ResearchSourceDraft> sources = new ArrayList<>();
        for (JsonNode result : root.path("results")) {
            ResearchSourceDraft source = toSource(result, "WEB", result.path("url").asText());
            if (source != null) {
                sources.add(source);
            }
        }
        return sources;
    }

    private ResearchSourceDraft toSource(JsonNode result, String sourceType, String fallbackUrl) {
        String url = text(result.path("url"));
        if (!StringUtils.hasText(url)) {
            url = fallbackUrl;
        }
        if (!isHttpUrl(url)) {
            return null;
        }
        ResearchSourceDraft source = new ResearchSourceDraft();
        source.setSourceType(sourceType);
        source.setUrl(url);
        source.setDomain(domain(url));
        source.setTitle(limit(firstNonBlank(text(result.path("title")), source.getDomain()), 255));
        source.setPublishedAt(parseDate(firstNonBlank(text(result.path("publishedDate")), text(result.path("published_date")))));
        source.setExcerpt(limit(extractExcerpt(result), 1600));
        return source;
    }

    private String extractExcerpt(JsonNode result) {
        JsonNode highlights = result.path("highlights");
        if (highlights.isArray() && !highlights.isEmpty()) {
            List<String> values = new ArrayList<>();
            for (JsonNode highlight : highlights) {
                if (highlight.isTextual()) {
                    values.add(highlight.asText());
                }
            }
            if (!values.isEmpty()) {
                return String.join(" ", values);
            }
        }
        return firstNonBlank(text(result.path("text")), text(result.path("summary")));
    }

    private LocalDateTime parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.exa.ai";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && StringUtils.hasText(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String domain(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String cleaned = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength) + "…";
    }
}
