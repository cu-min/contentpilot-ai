package com.aicontent.marketing.ai.service.impl;

import com.aicontent.marketing.ai.client.DeepSeekClient;
import com.aicontent.marketing.ai.service.AiModelService;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekAiModelService implements AiModelService {

    private final DeepSeekClient deepSeekClient;

    public DeepSeekAiModelService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return deepSeekClient.chat(systemPrompt, userPrompt);
    }
}
