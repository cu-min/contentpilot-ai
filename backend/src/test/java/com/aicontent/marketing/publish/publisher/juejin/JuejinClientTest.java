package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JuejinClientTest {

    private HttpServer server;
    private JuejinClient client;
    private JuejinAuthConfig config;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        String baseUrl = "http://127.0.0.1:%d".formatted(server.getAddress().getPort());
        client = new JuejinClient(new ObjectMapper(), HttpClient.newHttpClient(), baseUrl);
        config = new JuejinAuthConfig(
                "sid=ok",
                "JUnit",
                "",
                "2608",
                "",
                null,
                "6809637776263217160",
                List.of("6809640407484334093"),
                false,
                false,
                List.of(),
                List.of(),
                1077883,
                3
        );
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void createDraftParsesDraftId() {
        server.createContext("/article_draft/create", exchange -> respond(exchange, 200, """
                {"err_no":0,"err_msg":"success","data":{"id":"7658283231867879467"}}
                """));

        JuejinClient.JuejinDraftCreateResult result = client.createDraft(config);

        assertEquals("7658283231867879467", result.draftId());
    }

    @Test
    void publishArticleParsesArticleId() {
        server.createContext("/article/publish", exchange -> respond(exchange, 200, """
                {"err_no":0,"err_msg":"success","data":{"article_id":"7659027978949525547"}}
                """));

        JuejinClient.JuejinPublishResult result = client.publishArticle(config, "7659214017063878699");

        assertEquals("7659027978949525547", result.articleId());
    }

    @Test
    void publishArticleReportsJuejinErrorMessage() {
        server.createContext("/article/publish", exchange -> respond(exchange, 200, """
                {"err_no":1001,"err_msg":"category invalid","data":{}}
                """));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.publishArticle(config, "7659214017063878699")
        );

        assertEquals("掘金正式发布失败：category invalid", exception.getMessage());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
