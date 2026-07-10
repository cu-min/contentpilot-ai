package com.aicontent.marketing.publish.publisher.wechat;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WechatAccessTokenCache {

    private static final Duration REFRESH_AHEAD = Duration.ofMinutes(5);

    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    public String getToken(String appId, String appSecret, WechatClient client) {
        CachedToken cached = cache.get(appId);
        if (cached != null && cached.usableAt(Instant.now())) {
            return cached.accessToken();
        }

        WechatAccessTokenResponse response = client.fetchAccessToken(appId, appSecret);
        String accessToken = response.accessToken();
        if (StringUtils.hasText(accessToken)) {
            cache.put(appId, new CachedToken(
                    accessToken,
                    Instant.now().plusSeconds(Math.max(response.expiresIn(), 0))
            ));
        }
        return accessToken;
    }

    private record CachedToken(String accessToken, Instant expireAt) {

        private boolean usableAt(Instant now) {
            return StringUtils.hasText(accessToken) && now.isBefore(expireAt.minus(REFRESH_AHEAD));
        }
    }
}
