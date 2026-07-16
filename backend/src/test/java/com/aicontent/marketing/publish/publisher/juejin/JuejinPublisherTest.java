package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.publish.publisher.PublishContext;
import com.aicontent.marketing.publish.publisher.PublishResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JuejinPublisherTest {

    @Test
    void alwaysStopsAfterCreatingAndUpdatingDraft() {
        JuejinClient client = mock(JuejinClient.class);
        when(client.createDraft(any())).thenReturn(new JuejinClient.JuejinDraftCreateResult("draft-123"));
        when(client.updateDraft(any(), eq("draft-123"), any()))
                .thenReturn(new JuejinClient.JuejinDraftUpdateResult("draft-123"));
        JuejinPublisher publisher = new JuejinPublisher(client, new ObjectMapper());

        PublishResult result = publisher.publish(context());

        assertTrue(result.prepared());
        assertEquals("draft-123", result.draftId());
        assertEquals("https://juejin.cn/editor/drafts/draft-123", result.draftUrl());
    }

    private PublishContext context() {
        return new PublishContext(
                1L, 1L, 1L, "JUEJIN", 1L, "title", "content", "summary", "", "",
                "UNOFFICIAL_API", null, null, null, true,
                """
                {
                  "cookie": "sid=ok",
                  "defaultCategoryId": "category-1",
                  "defaultTagIds": ["tag-1"],
                  "draftOnly": false
                }
                """,
                "account", "", null
        );
    }
}
