package com.aicontent.marketing.publish.publisher.wechat;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatAuthConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseUsesDefaultsForOptionalFields() {
        WechatAuthConfig config = WechatAuthConfig.parse("""
                {
                  "appId": "wx123",
                  "appSecret": "secret",
                  "defaultThumbMediaId": "thumb-media-id"
                }
                """, objectMapper);

        assertEquals("wx123", config.appId());
        assertEquals("secret", config.appSecret());
        assertEquals("thumb-media-id", config.defaultThumbMediaId());
        assertEquals("", config.author());
        assertEquals(true, config.draftOnly());
        assertEquals(0, config.needOpenComment());
        assertEquals(0, config.onlyFansCanComment());
        assertEquals("", config.sourceUrl());
    }

    @Test
    void parseRequiresAppIdAppSecretAndThumbMediaId() {
        BusinessException missingAppId = assertThrows(BusinessException.class, () -> WechatAuthConfig.parse("""
                {
                  "appSecret": "secret",
                  "defaultThumbMediaId": "thumb-media-id"
                }
                """, objectMapper));
        assertEquals("微信公众号 AppID 未配置", missingAppId.getMessage());

        BusinessException missingAppSecret = assertThrows(BusinessException.class, () -> WechatAuthConfig.parse("""
                {
                  "appId": "wx123",
                  "defaultThumbMediaId": "thumb-media-id"
                }
                """, objectMapper));
        assertEquals("微信公众号 AppSecret 未配置", missingAppSecret.getMessage());

        BusinessException missingThumb = assertThrows(BusinessException.class, () -> WechatAuthConfig.parse("""
                {
                  "appId": "wx123",
                  "appSecret": "secret"
                }
                """, objectMapper));
        assertEquals("微信公众号默认封面素材 media_id 未配置", missingThumb.getMessage());
    }

    @Test
    void parseReportsInvalidJson() {
        BusinessException exception = assertThrows(BusinessException.class, () -> WechatAuthConfig.parse("{", objectMapper));

        assertEquals("微信公众号认证配置格式错误", exception.getMessage());
    }
}
