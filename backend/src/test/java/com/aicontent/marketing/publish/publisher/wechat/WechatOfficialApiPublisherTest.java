package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.aicontent.marketing.publish.service.markdown.MarkdownToWechatHtmlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatOfficialApiPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void draftOnlyTrueCreatesDraftForManualConfirmation() {
        FakeWechatClient client = new FakeWechatClient();
        WechatOfficialApiPublisher publisher = publisher(client);

        PublishResult result = publisher.publish(context(true));

        assertTrue(result.prepared());
        assertEquals("wechat-draft:draft-media-id", result.draftUrl());
        assertEquals("draft-media-id", result.draftId());
        assertEquals(0, client.submitCount);
    }

    @Test
    void draftOnlyFalseStillNeverSubmitsFreePublish() {
        FakeWechatClient client = new FakeWechatClient();
        WechatOfficialApiPublisher publisher = publisher(client);

        PublishResult result = publisher.publish(context(false));

        assertTrue(result.prepared());
        assertEquals("draft-media-id", result.draftId());
        assertEquals("wechat-draft:draft-media-id", result.draftUrl());
        assertEquals(0, client.submitCount);
    }

    private WechatOfficialApiPublisher publisher(FakeWechatClient client) {
        return new WechatOfficialApiPublisher(
                client,
                new WechatAccessTokenCache(),
                new MarkdownToWechatHtmlService(),
                objectMapper
        );
    }

    private PublishContext context(boolean draftOnly) {
        return new PublishContext(
                1L,
                1L,
                1L,
                "WECHAT_OFFICIAL",
                1L,
                "title",
                "# content",
                "summary",
                "",
                "",
                "OFFICIAL_API",
                null,
                null,
                null,
                true,
                """
                {
                  "appId": "wx123",
                  "appSecret": "secret",
                  "defaultThumbMediaId": "thumb-media-id",
                  "draftOnly": %s
                }
                """.formatted(draftOnly),
                "account",
                "",
                null
        );
    }

    private static class FakeWechatClient extends WechatClient {

        private int submitCount;

        private FakeWechatClient() {
            super(new ObjectMapper(), HttpClient.newHttpClient(), "https://wechat.test/cgi-bin");
        }

        @Override
        public WechatAccessTokenResponse fetchAccessToken(String appId, String appSecret) {
            return new WechatAccessTokenResponse("access-token", 7200);
        }

        @Override
        public WechatDraftAddResponse createDraft(String accessToken, WechatDraftAddRequest requestBody) {
            return new WechatDraftAddResponse("draft-media-id");
        }

    }
}
