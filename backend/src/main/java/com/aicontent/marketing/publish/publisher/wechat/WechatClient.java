package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Component
public class WechatClient {

    private static final Logger log = LoggerFactory.getLogger(WechatClient.class);
    private static final String DEFAULT_BASE_URL = "https://api.weixin.qq.com/cgi-bin";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_ERROR_LENGTH = 300;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final RestOperations restOperations;
    private final String baseUrl;

    @Autowired
    public WechatClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build(), new RestTemplate(), DEFAULT_BASE_URL);
    }

    WechatClient(ObjectMapper objectMapper, HttpClient httpClient, String baseUrl) {
        this(objectMapper, httpClient, new RestTemplate(), baseUrl);
    }

    WechatClient(ObjectMapper objectMapper, HttpClient httpClient, RestOperations restOperations, String baseUrl) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.restOperations = restOperations;
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

    public WechatMaterialUploadResponse uploadPermanentImageMaterial(String accessToken, MultipartFile file) {
        try {
            String url = baseUrl + "/material/add_material?access_token=" + encode(accessToken) + "&type=image";
            ResponseEntity<String> response = restOperations.exchange(
                    url,
                    HttpMethod.POST,
                    buildMultipartRequest(file),
                    String.class
            );
            log.info("Wechat material/add_material request completed: status={}", response.getStatusCode().value());
            JsonNode root = readSuccessfulBody(
                    response.getStatusCode().value(),
                    response.getBody(),
                    "上传微信公众号默认封面失败",
                    true
            );
            String mediaId = root.path("media_id").asText();
            if (!StringUtils.hasText(mediaId)) {
                throw new BusinessException("上传微信公众号默认封面失败：响应中缺少 media_id");
            }
            return new WechatMaterialUploadResponse(mediaId, root.path("url").asText(""));
        } catch (RestClientResponseException exception) {
            String bodySummary = truncate(exception.getResponseBodyAsString(StandardCharsets.UTF_8));
            log.warn("Wechat material/add_material request failed: status={}, body={}",
                    exception.getStatusCode().value(), bodySummary);
            throw new BusinessException("上传微信公众号默认封面失败，HTTP 状态码："
                    + exception.getStatusCode().value() + "，响应：" + bodySummary);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("上传微信公众号默认封面失败：" + safeMessage(exception));
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> buildMultipartRequest(MultipartFile file) throws Exception {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(parseContentType(file.getContentType()));
        partHeaders.setContentDisposition(ContentDisposition.formData()
                .name("media")
                .filename(safeCoverFilename(file.getOriginalFilename()))
                .build());

        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return safeCoverFilename(file.getOriginalFilename());
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("media", new HttpEntity<>(resource, partHeaders));
        return new HttpEntity<>(body, requestHeaders);
    }

    private JsonNode readSuccessfulBody(HttpResponse<String> response, String failurePrefix) throws Exception {
        return readSuccessfulBody(response.statusCode(), response.body(), failurePrefix, false);
    }

    private JsonNode readSuccessfulBody(
            int statusCode,
            String body,
            String failurePrefix,
            boolean simpleWechatError
    ) throws Exception {
        if (statusCode < 200 || statusCode >= 300) {
            throw new BusinessException(failurePrefix + "，HTTP 状态码：" + statusCode + "，响应：" + truncate(body));
        }
        JsonNode root = objectMapper.readTree(body);
        if (root.has("errcode") && root.path("errcode").asInt(0) != 0) {
            String errMsg = root.path("errmsg").asText("unknown error");
            if (simpleWechatError) {
                throw new BusinessException(failurePrefix + "：" + truncate(errMsg));
            }
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

    private MediaType parseContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String safeCoverFilename(String originalFilename) {
        String extension = "jpg";
        if (StringUtils.hasText(originalFilename)) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            }
        }
        return "cover." + extension;
    }
}
