package com.aicontent.marketing.publish.service.markdown;

import com.aicontent.marketing.common.exception.BusinessException;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarkdownToWechatHtmlService {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public String convert(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            throw new BusinessException("公众号正文内容不能为空");
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
