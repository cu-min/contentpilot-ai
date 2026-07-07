package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    private final CloseableHttpClient apacheHttpClient;
    private final String baseUrl;

    @Autowired
    public WechatClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build(), HttpClients.createDefault(), DEFAULT_BASE_URL);
    }

    WechatClient(ObjectMapper objectMapper, HttpClient httpClient, String baseUrl) {
        this(objectMapper, httpClient, HttpClients.createDefault(), baseUrl);
    }

    WechatClient(
            ObjectMapper objectMapper,
            HttpClient httpClient,
            CloseableHttpClient apacheHttpClient,
            String baseUrl
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.apacheHttpClient = apacheHttpClient;
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

    public WechatFreePublishSubmitResponse submitFreePublish(String accessToken, String mediaId) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("media_id", mediaId);
            String url = baseUrl + "/freepublish/submit?access_token=" + encode(accessToken);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Wechat freepublish/submit request completed: status={}, body={}",
                    response.statusCode(), truncate(response.body()));
            JsonNode root = readSuccessfulBody(response.statusCode(), response.body(), "提交微信公众号发布失败", true);
            String publishId = root.path("publish_id").asText();
            if (!StringUtils.hasText(publishId)) {
                throw new BusinessException("提交微信公众号发布失败：响应中缺少 publish_id");
            }
            return new WechatFreePublishSubmitResponse(publishId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("提交微信公众号发布失败：" + safeMessage(exception));
        }
    }

    public WechatPublishStatusResult getFreePublishStatus(String accessToken, String publishId) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("publish_id", publishId);
            String url = baseUrl + "/freepublish/get?access_token=" + encode(accessToken);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Wechat freepublish/get request completed: status={}, body={}",
                    response.statusCode(), truncate(response.body()));
            JsonNode root = readSuccessfulBody(response.statusCode(), response.body(), "查询微信公众号发布状态失败", true);
            return parsePublishStatus(root, truncate(response.body()));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("查询微信公众号发布状态失败：" + safeMessage(exception));
        }
    }

    public WechatMaterialUploadResponse uploadPermanentImageMaterial(String accessToken, MultipartFile file) {
        try {
            String url = baseUrl + "/material/add_material?access_token=" + encode(accessToken) + "&type=image";
            byte[] fileBytes = file.getBytes();
            String filename = safeCoverFilename(file.getOriginalFilename());
            ContentType contentType = parseApacheContentType(file.getContentType(), filename);
            org.apache.hc.core5.http.HttpEntity entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.LEGACY)
                    .addBinaryBody("media", fileBytes, contentType, filename)
                    .build();
            if (entity.getContentLength() <= 0 || entity.isChunked()) {
                throw new BusinessException("上传微信公众号默认封面失败：multipart 请求体构造异常");
            }
            log.info(
                    "Wechat material upload request prepared: fileSize={}, filename={}, contentType={}, multipartContentLength={}, chunked={}",
                    fileBytes.length,
                    filename,
                    contentType,
                    entity.getContentLength(),
                    entity.isChunked()
            );

            HttpPost post = new HttpPost(url);
            post.setConfig(RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofSeconds(TIMEOUT.toSeconds()))
                    .setConnectionRequestTimeout(Timeout.ofSeconds(TIMEOUT.toSeconds()))
                    .build());
            post.setEntity(entity);

            WechatHttpResponse response = apacheHttpClient.execute(post, httpResponse -> new WechatHttpResponse(
                    httpResponse.getCode(),
                    httpResponse.getEntity() == null
                            ? ""
                            : EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8)
            ));
            log.info("Wechat material/add_material request completed: status={}, body={}",
                    response.statusCode(), truncate(response.body()));
            JsonNode root = readSuccessfulBody(
                    response.statusCode(),
                    response.body(),
                    "上传微信公众号默认封面失败",
                    true
            );
            String mediaId = root.path("media_id").asText();
            if (!StringUtils.hasText(mediaId)) {
                throw new BusinessException("上传微信公众号默认封面失败：响应中缺少 media_id");
            }
            return new WechatMaterialUploadResponse(mediaId, root.path("url").asText(""));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("上传微信公众号默认封面失败：" + safeMessage(exception));
        }
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

    private WechatPublishStatusResult parsePublishStatus(JsonNode root, String rawSummary) {
        Integer publishStatus = root.has("publish_status") && root.path("publish_status").canConvertToInt()
                ? root.path("publish_status").asInt()
                : null;
        String articleId = firstText(root, "article_id");
        String articleUrl = firstText(root, "article_url");
        JsonNode item = root.path("article_detail").path("item");
        if (item.isArray() && !item.isEmpty()) {
            JsonNode firstItem = item.get(0);
            if (!StringUtils.hasText(articleId)) {
                articleId = firstText(firstItem, "article_id");
            }
            if (!StringUtils.hasText(articleUrl)) {
                articleUrl = firstText(firstItem, "article_url");
            }
        }

        if (Integer.valueOf(0).equals(publishStatus)) {
            if (!StringUtils.hasText(articleUrl)) {
                return WechatPublishStatusResult.failed(
                        publishStatus,
                        "未获取到微信正式文章链接：" + rawSummary,
                        rawSummary
                );
            }
            return WechatPublishStatusResult.success(publishStatus, articleId, articleUrl, rawSummary);
        }
        if (Integer.valueOf(1).equals(publishStatus)) {
            return WechatPublishStatusResult.processing(publishStatus, rawSummary);
        }
        if (Integer.valueOf(2).equals(publishStatus) || Integer.valueOf(3).equals(publishStatus)) {
            return WechatPublishStatusResult.failed(
                    publishStatus,
                    "微信发布失败：" + firstNonBlank(firstText(root, "errmsg"), rawSummary),
                    rawSummary
            );
        }
        return WechatPublishStatusResult.failed(
                publishStatus,
                "未知发布状态：" + rawSummary,
                rawSummary
        );
    }

    private String firstText(JsonNode root, String field) {
        JsonNode value = root.path(field);
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            return value.asText();
        }
        if (value.isNumber()) {
            return value.asText();
        }
        return "";
    }

    private String firstNonBlank(String first, String fallback) {
        return StringUtils.hasText(first) ? first : fallback;
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

    private ContentType parseApacheContentType(String contentType, String filename) {
        if (!StringUtils.hasText(contentType)) {
            return contentTypeFromFilename(filename);
        }
        try {
            return ContentType.parse(contentType);
        } catch (Exception ignored) {
            return contentTypeFromFilename(filename);
        }
    }

    private ContentType contentTypeFromFilename(String filename) {
        if (filename.endsWith(".png")) {
            return ContentType.IMAGE_PNG;
        }
        if (filename.endsWith(".gif")) {
            return ContentType.IMAGE_GIF;
        }
        if (filename.endsWith(".bmp")) {
            return ContentType.create("image/bmp");
        }
        return ContentType.IMAGE_JPEG;
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

    private record WechatHttpResponse(int statusCode, String body) {
    }
}
