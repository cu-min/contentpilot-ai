package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void wechatErrorBecomesBusinessException() {
        WechatClient client = new WechatClient(objectMapper, new StubHttpClient("""
                {"errcode":40013,"errmsg":"invalid appid"}
                """), "https://wechat.test/cgi-bin");

        BusinessException exception = assertThrows(BusinessException.class, () -> client.fetchAccessToken("wx123", "secret"));

        assertEquals("获取微信 access_token 失败，微信接口返回错误：invalid appid", exception.getMessage());
    }

    private static class StubHttpClient extends HttpClient {

        private final String body;

        private StubHttpClient(String body) {
            this.body = body;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
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
}
