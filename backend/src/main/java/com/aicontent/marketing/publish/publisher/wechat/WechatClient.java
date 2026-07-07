package com.aicontent.marketing.publish.publisher.wechat;

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

@Component
public class WechatClient {

    private static final Logger log = LoggerFactory.getLogger(WechatClient.class);
    private static final String DEFAULT_BASE_URL = "https://api.weixin.qq.com/cgi-bin";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_ERROR_LENGTH = 300;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;

    @Autowired
    public WechatClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build(), DEFAULT_BASE_URL);
    }

    WechatClient(ObjectMapper objectMapper, HttpClient httpClient, String baseUrl) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    public WechatAccessTokenResponse fetchAccessToken(String appId, String appSecret) {
        try {
            String url = baseUrl + "/token?grant_type=client_credential"
                    + "&appid=" + encode(appId)
                    + "&secret=" + encode(appSecret);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Wechat access_token request completed: status={}", response.statusCode());
            JsonNode root = readSuccessfulBody(response, "获取微信 access_token 失败");
            String accessToken = root.path("access_token").asText();
            long expiresIn = root.path("expires_in").asLong(0);
            if (!StringUtils.hasText(accessToken) || expiresIn <= 0) {
                throw new BusinessException("获取微信 access_token 失败：响应中缺少 access_token");
            }
            return new WechatAccessTokenResponse(accessToken, expiresIn);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("获取微信 access_token 失败：" + safeMessage(exception));
        }
    }

    public WechatDraftAddResponse createDraft(String accessToken, WechatDraftAddRequest requestBody) {
        try {
            String url = baseUrl + "/draft/add?access_token=" + encode(accessToken);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Wechat draft/add request completed: status={}", response.statusCode());
            JsonNode root = readSuccessfulBody(response, "创建微信公众号草稿失败");
            String mediaId = root.path("media_id").asText();
            if (!StringUtils.hasText(mediaId)) {
                throw new BusinessException("创建微信公众号草稿失败：响应中缺少 media_id");
            }
            return new WechatDraftAddResponse(mediaId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("创建微信公众号草稿失败：" + safeMessage(exception));
        }
    }

    private JsonNode readSuccessfulBody(HttpResponse<String> response, String failurePrefix) throws Exception {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(failurePrefix + "，HTTP 状态码：" + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("errcode") && root.path("errcode").asInt(0) != 0) {
            String errMsg = root.path("errmsg").asText("unknown error");
            throw new BusinessException(failurePrefix + "，微信接口返回错误：" + truncate(errMsg));
        }
        return root;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safeMessage(Exception exception) {
        return truncate(StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : exception.getClass().getSimpleName());
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
