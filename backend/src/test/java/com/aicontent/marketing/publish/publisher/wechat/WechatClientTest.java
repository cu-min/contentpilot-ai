package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fetchAccessTokenReadsSuccessfulResponse() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"access_token":"token-value","expires_in":7200}
                """), "https://wechat.test/cgi-bin");

        WechatAccessTokenResponse response = client.fetchAccessToken("wx123", "secret");

        assertEquals("token-value", response.accessToken());
        assertEquals(7200, response.expiresIn());
    }

    @Test
    void createDraftReadsMediaId() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"media_id":"draft-media-id"}
                """), "https://wechat.test/cgi-bin");

        WechatDraftAddResponse response = client.createDraft("token-value", new WechatDraftAddRequest(
                java.util.List.of(new WechatDraftAddRequest.Article(
                        "title",
                        "author",
                        "digest",
                        "<p>content</p>",
                        "thumb-media-id",
                        "",
                        0,
                        0
                ))
        ));

        assertEquals("draft-media-id", response.mediaId());
    }

    @Test
    void submitFreePublishReadsPublishId() {
        StubHttpClient httpClient = new StubHttpClient("""
                {"errcode":0,"publish_id":"publish-id"}
                """);
        WechatClient client = new WechatClient(objectMapper, httpClient, "https://wechat.test/cgi-bin");

        WechatFreePublishSubmitResponse response = client.submitFreePublish("token-value", "draft-media-id");

        assertEquals("publish-id", response.publishId());
        assertTrue(httpClient.lastRequest.uri().toString().contains("/freepublish/submit"));
        assertTrue(httpClient.lastRequest.uri().toString().contains("access_token=token-value"));
    }

    @Test
    void submitFreePublishWechatErrorBecomesBusinessException() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"errcode":48001,"errmsg":"api unauthorized"}
                """), "https://wechat.test/cgi-bin");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.submitFreePublish("token-value", "draft-media-id")
        );

        assertEquals("提交微信公众号发布失败：api unauthorized", exception.getMessage());
    }

    @Test
    void getFreePublishStatusReadsSuccessArticleUrl() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"errcode":0,"publish_status":0,"article_id":"article-id","article_detail":{"item":[{"article_url":"https://mp.weixin.qq.com/s/article"}]}}
                """), "https://wechat.test/cgi-bin");

        WechatPublishStatusResult result = client.getFreePublishStatus("token-value", "publish-id");

        assertTrue(result.success());
        assertEquals("article-id", result.articleId());
        assertEquals("https://mp.weixin.qq.com/s/article", result.articleUrl());
    }

    @Test
    void getFreePublishStatusReadsProcessing() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"errcode":0,"publish_status":1}
                """), "https://wechat.test/cgi-bin");

        WechatPublishStatusResult result = client.getFreePublishStatus("token-value", "publish-id");

        assertTrue(result.processing());
        assertEquals(1, result.publishStatus());
    }

    @Test
    void getFreePublishStatusReadsFailedStatus() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"errcode":0,"publish_status":2,"errmsg":"publish failed"}
                """), "https://wechat.test/cgi-bin");

        WechatPublishStatusResult result = client.getFreePublishStatus("token-value", "publish-id");

        assertTrue(result.failed());
        assertTrue(result.errorMessage().contains("publish failed"));
    }

    @Test
    void uploadPermanentImageMaterialSendsCurlLikeMultipartAndReadsMediaIdAndUrl() throws Exception {
        try (LocalWechatServer server = LocalWechatServer.success("""
                {"media_id":"cover-media-id","url":"https://mmbiz.qpic.cn/cover.jpg"}
                """)) {
            WechatClient client = new WechatClient(
                    objectMapper,
                    new StubHttpClient("{}"),
                    HttpClients.createDefault(),
                    server.baseUrl()
            );
            MockMultipartFile file = new MockMultipartFile("file", "截图.png", "image/png", "image-bytes".getBytes());

            WechatMaterialUploadResponse response = client.uploadPermanentImageMaterial("token-value", file);

            CapturedRequest request = server.captured();
            assertEquals("POST", request.method());
            assertEquals("/cgi-bin/material/add_material?access_token=token-value&type=image", request.rawPathAndQuery());
            assertTrue(request.firstHeader("Content-Type").startsWith("multipart/form-data"));
            assertTrue(request.firstHeader("Content-Type").contains("boundary="));
            assertEquals(null, request.firstHeader("Transfer-Encoding"));
            assertTrue(Long.parseLong(request.firstHeader("Content-Length")) > 0);
            assertTrue(request.body().contains("name=\"media\""));
            assertTrue(request.body().contains("filename=\"cover.png\""));
            assertTrue(request.body().contains("Content-Type: image/png"));
            assertTrue(request.body().contains("image-bytes"));
            assertEquals("cover-media-id", response.mediaId());
            assertEquals("https://mmbiz.qpic.cn/cover.jpg", response.url());
        }
    }

    @Test
    void uploadPermanentImageMaterialUsesSimpleWechatErrorMessage() throws Exception {
        try (LocalWechatServer server = LocalWechatServer.success("""
                {"errcode":40007,"errmsg":"invalid media_id"}
                """)) {
            WechatClient client = new WechatClient(
                    objectMapper,
                    new StubHttpClient("{}"),
                    HttpClients.createDefault(),
                    server.baseUrl()
            );
            MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "image-bytes".getBytes());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> client.uploadPermanentImageMaterial("token-value", file)
            );

            assertEquals("上传微信公众号默认封面失败：invalid media_id", exception.getMessage());
        }
    }

    @Test
    void uploadPermanentImageMaterialIncludesHttpErrorBody() throws Exception {
        try (LocalWechatServer server = LocalWechatServer.error(412, """
                {"errcode":41005,"errmsg":"media data missing"}
                """)) {
            WechatClient client = new WechatClient(
                    objectMapper,
                    new StubHttpClient("{}"),
                    HttpClients.createDefault(),
                    server.baseUrl()
            );
            MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "image-bytes".getBytes());

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> client.uploadPermanentImageMaterial("token-value", file)
            );

            assertTrue(exception.getMessage().contains("HTTP 状态码：412"));
            assertTrue(exception.getMessage().contains("media data missing"));
        }
    }

    @Test
    void wechatErrorBecomesBusinessException() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"errcode":40013,"errmsg":"invalid appid"}
                """), "https://wechat.test/cgi-bin");

        BusinessException exception = assertThrows(BusinessException.class, () -> client.fetchAccessToken("wx123", "secret"));

        assertEquals("获取微信 access_token 失败，微信接口返回错误：invalid appid", exception.getMessage());
    }

    private static class StubHttpClient extends HttpClient {

        private final String body;
        private HttpRequest lastRequest;

        private StubHttpClient(String body) {
            this.body = body;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            this.lastRequest = request;
            return new StubHttpResponse<>(200, (T) body);
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<java.time.Duration> connectTimeout() {
            return java.util.Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public java.util.Optional<java.net.ProxySelector> proxy() {
            return java.util.Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            return null;
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters() {
            return null;
        }

        @Override
        public java.util.Optional<java.net.Authenticator> authenticator() {
            return java.util.Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public java.util.Optional<java.util.concurrent.Executor> executor() {
            return java.util.Optional.empty();
        }
    }

    private record StubHttpResponse<T>(int statusCode, T body) implements HttpResponse<T> {

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public java.util.Optional<HttpResponse<T>> previousResponse() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
        }

        @Override
        public java.net.URI uri() {
            return java.net.URI.create("https://wechat.test");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
            return java.util.Optional.empty();
        }
    }

    private static class LocalWechatServer implements AutoCloseable {

        private final HttpServer server;
        private final AtomicReference<CapturedRequest> captured = new AtomicReference<>();

        private LocalWechatServer(int status, String responseBody) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/cgi-bin/material/add_material", exchange -> handle(exchange, status, responseBody));
            server.start();
        }

        private static LocalWechatServer success(String responseBody) throws IOException {
            return new LocalWechatServer(200, responseBody);
        }

        private static LocalWechatServer error(int status, String responseBody) throws IOException {
            return new LocalWechatServer(status, responseBody);
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/cgi-bin";
        }

        private CapturedRequest captured() {
            return captured.get();
        }

        private void handle(HttpExchange exchange, int status, String responseBody) throws IOException {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            exchange.getRequestBody().transferTo(body);
            captured.set(new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().toString(),
                    exchange.getRequestHeaders(),
                    body.toString(StandardCharsets.UTF_8)
            ));
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private record CapturedRequest(
            String method,
            String rawPathAndQuery,
            Map<String, List<String>> headers,
            String body
    ) {

        private String firstHeader(String name) {
            List<String> values = headers.get(name);
            if (values == null) {
                values = headers.entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }
}
