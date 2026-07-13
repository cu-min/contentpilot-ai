package com.aicontent.marketing.ai.parser;

import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiJsonParser {

    private final ObjectMapper objectMapper;

    public AiJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiGeneratedArticle parseGeneratedArticle(String rawResult) {
        try {
            AiGeneratedArticle article = objectMapper.readValue(extractJsonObject(rawResult), AiGeneratedArticle.class);
            if (!StringUtils.hasText(article.getTitle()) || !StringUtils.hasText(article.getContent())) {
                throw new BusinessException("AI 返回格式解析失败，请重试");
            }
            return article;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("AI 返回格式解析失败，请重试");
        }
    }

    private String extractJsonObject(String rawResult) {
        if (!StringUtils.hasText(rawResult)) {
            throw new BusinessException("AI 返回内容为空，请重试");
        }

        String content = rawResult.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7).trim();
        } else if (content.startsWith("```")) {
            content = content.substring(3).trim();
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3).trim();
        }

        int start = content.indexOf('{');
        if (start < 0) {
            throw new BusinessException("AI 返回格式解析失败，请重试");
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int index = start; index < content.length(); index++) {
            char current = content.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return content.substring(start, index + 1);
            }
        }
        throw new BusinessException("AI 返回格式解析失败，请重试");
    }
}
