package com.aicontent.marketing.ai.research;

import com.aicontent.marketing.ai.config.ExaProperties;
import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExaResearchClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private TestExaProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        properties = new TestExaProperties();
        ReflectionTestUtils.setField(properties, "baseUrl", "http://127.0.0.1:%d".formatted(server.getAddress().getPort()));
        ReflectionTestUtils.setField(properties, "timeoutSeconds", 2);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void searchSendsLimitsAndDateRangeThenReturnsSanitizedSource() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        server.createContext("/search", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            respond(exchange, 200, """
                    {"results":[{"title":"Example source","url":"https://example.com/news","publishedDate":"2025-03-05T00:00:00Z","highlights":["first\\nline", "second line"]}]}
                    """);
        });
        ExaResearchClient client = new ExaResearchClient(properties, objectMapper);
        ResearchPlan plan = new ResearchPlan();
        plan.setQuery("AI news");
        plan.setStartPublishedDate(java.time.Instant.parse("2025-03-01T00:00:00Z"));
        plan.setEndPublishedDate(java.time.Instant.parse("2025-03-08T00:00:00Z"));

        List<ResearchSourceDraft> sources = client.search(plan);

        JsonNode body = objectMapper.readTree(requestBody.get());
        assertEquals("test-exa-key", apiKey.get());
        assertEquals(10, body.path("numResults").asInt());
        assertTrue(body.path("contents").path("highlights").asBoolean());
        assertEquals("2025-03-01T00:00:00Z", body.path("startPublishedDate").asText());
        assertEquals("2025-03-08T00:00:00Z", body.path("endPublishedDate").asText());
        assertEquals(1, sources.size());
        assertEquals("WEB", sources.get(0).getSourceType());
        assertEquals("first line second line", sources.get(0).getExcerpt());
    }

    @Test
    void searchReportsUnauthorizedKey() {
        server.createContext("/search", exchange -> respond(exchange, 401, "{}"));
        ExaResearchClient client = new ExaResearchClient(properties, objectMapper);

        BusinessException exception = assertThrows(BusinessException.class, () -> client.search(new ResearchPlan()));

        assertEquals("Exa API Key 无效或没有权限", exception.getMessage());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static class TestExaProperties extends ExaProperties {
        @Override
        public String getApiKey() {
            return "test-exa-key";
        }
    }
}
