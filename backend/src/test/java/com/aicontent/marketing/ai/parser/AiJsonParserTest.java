package com.aicontent.marketing.ai.parser;

import com.aicontent.marketing.ai.vo.AiGeneratedArticle;
import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiJsonParserTest {

    private final AiJsonParser parser = new AiJsonParser(new ObjectMapper());

    @Test
    void parsesJsonWrappedInMarkdownAndExtraText() {
        AiGeneratedArticle article = parser.parseGeneratedArticle("""
                下面是生成结果：
                ```json
                {"title":"标题","summary":"摘要","content":"正文 { 保留 }","tags":["标签"],"keywords":["关键词"]}
                ```
                请查收。
                """);

        assertEquals("标题", article.getTitle());
        assertEquals("正文 { 保留 }", article.getContent());
    }

    @Test
    void rejectsAnIncompleteJsonObject() {
        assertThrows(BusinessException.class,
                () -> parser.parseGeneratedArticle("结果：{\"title\":\"标题\""));
    }
}
