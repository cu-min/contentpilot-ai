package com.aicontent.marketing.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekProperties {

    @Value("${DEEPSEEK_API_URL:${DEEPSEEK_BASE_URL:https://api.deepseek.com}}")
    private String baseUrl;

    @Value("${DEEPSEEK_MODEL:deepseek-chat}")
    private String model;

    @Value("${DEEPSEEK_TIMEOUT_SECONDS:60}")
    private Integer timeoutSeconds;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return System.getenv("DEEPSEEK_API_KEY");
    }

    public String getModel() {
        return model;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
