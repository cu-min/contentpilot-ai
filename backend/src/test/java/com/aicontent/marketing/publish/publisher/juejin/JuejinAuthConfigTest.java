package com.aicontent.marketing.publish.publisher.juejin;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JuejinAuthConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseDoesNotRequireLegacyDraftId() {
        JuejinAuthConfig config = assertDoesNotThrow(() -> JuejinAuthConfig.parse("""
                {
                  "cookie": "sid=ok",
                  "defaultCategoryId": "6809637776263217160",
                  "defaultTagIds": ["6809640407484334093"],
                  "draftOnly": false
                }
                """, objectMapper));

        assertEquals("6809637776263217160", config.defaultCategoryId());
        assertEquals("6809640407484334093", config.defaultTagIds().get(0));
    }

    @Test
    void parseRequiresCookieAndCategory() {
        BusinessException missingCookie = assertThrows(BusinessException.class, () -> JuejinAuthConfig.parse("""
                {
                  "defaultCategoryId": "6809637776263217160",
                  "defaultTagIds": ["6809640407484334093"]
                }
                """, objectMapper));
        assertEquals("掘金 Cookie 未配置，请在平台账号认证配置中填写 cookie", missingCookie.getMessage());

        BusinessException missingCategory = assertThrows(BusinessException.class, () -> JuejinAuthConfig.parse("""
                {
                  "cookie": "sid=ok",
                  "defaultTagIds": ["6809640407484334093"]
                }
                """, objectMapper));
        assertEquals("掘金默认分类 ID 未配置，请从 article_draft/update 请求 Payload 的 category_id 获取", missingCategory.getMessage());
    }

    @Test
    void parseRequiresTagForAutomaticPublish() {
        BusinessException exception = assertThrows(BusinessException.class, () -> JuejinAuthConfig.parse("""
                {
                  "cookie": "sid=ok",
                  "defaultCategoryId": "6809637776263217160",
                  "draftOnly": false
                }
                """, objectMapper));

        assertEquals("掘金自动发布需要至少配置一个默认标签 ID", exception.getMessage());
    }
}
