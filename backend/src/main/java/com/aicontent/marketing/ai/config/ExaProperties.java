package com.aicontent.marketing.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExaProperties {

    @Value("${EXA_BASE_URL:https://api.exa.ai}")
    private String baseUrl;

    @Value("${EXA_TIMEOUT_SECONDS:15}")
    private Integer timeoutSeconds;

    public String getBaseUrl() {
        return baseUrl;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getApiKey() {
        return System.getenv("EXA_API_KEY");
    }
}
