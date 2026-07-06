package com.aicontent.marketing.ai.prompt;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptBuilder {

    public String buildSystemPrompt() {
        return """
                你是一名专业的内容营销运营人员，擅长把产品信息转化为清晰、可信、不过度硬广的营销文章。
                你必须严格按照用户要求输出内容，并保证输出可被后端程序稳定解析。
                输出要求：
                1. 只返回 JSON，不要返回 Markdown 代码块包裹。
                2. 不要返回额外解释文字。
                3. JSON 字段必须包含 title、summary、content、tags、keywords。
                4. tags 和 keywords 必须是字符串数组。
                5. content 字段内部可以是 Markdown 字符串。
                """;
    }

    public String buildUserPrompt(ProductConfigVO productConfig, AiArticleGenerateRequest request) {
        if (productConfig == null || !StringUtils.hasText(productConfig.getProductName())) {
            return buildTopicOnlyPrompt(request);
        }
        return """
                请基于以下产品配置和写作要求生成一篇原始文章。

                【产品配置】
                产品名称：%s
                产品简介：%s
                官网链接：%s
                核心功能：%s
                目标用户：%s
                产品优势：%s
                品牌语气：%s
                禁用词/敏感词：%s

                【文章要求】
                文章主题：%s
                文章类型：%s
                语言：%s
                额外要求：%s

                【写作规则】
                1. 文章要围绕产品配置展开，但不要过度硬广。
                2. 中文语言请输出中文，英文语言请输出英文。
                3. Markdown 正文结构要清晰，包含标题、小标题和段落。
                4. 根据文章类型采用合适写法：产品介绍强调价值；教程强调步骤；行业科普强调知识解释；竞品对比强调客观比较；解决方案强调场景和路径；SEO文章强调搜索关键词覆盖。
                5. 避免使用禁用词/敏感词。
                6. 返回 JSON 格式如下：
                {
                  "title": "...",
                  "summary": "...",
                  "content": "...",
                  "tags": ["标签1", "标签2"],
                  "keywords": ["关键词1", "关键词2"]
                }
                """.formatted(
                text(productConfig.getProductName()),
                text(productConfig.getProductIntro()),
                text(productConfig.getOfficialUrl()),
                text(productConfig.getCoreFeatures()),
                text(productConfig.getTargetUsers()),
                text(productConfig.getAdvantages()),
                text(productConfig.getBrandTone()),
                text(productConfig.getBannedWords()),
                text(request.getTopic()),
                text(request.getType()),
                "ZH".equals(request.getLanguage()) ? "中文" : "英文",
                text(request.getExtraRequirement())
        );
    }

    private String buildTopicOnlyPrompt(AiArticleGenerateRequest request) {
        return """
                请基于以下写作要求生成一篇原始文章。

                【文章要求】
                文章主题：%s
                文章类型：%s
                语言：%s
                额外要求：%s

                【写作规则】
                1. 当前没有选择关联产品，不要编造具体产品名称、官网链接、品牌能力或不存在的功能。
                2. 请围绕文章主题本身展开，输出通用但具体、有信息量的内容。
                3. 中文语言请输出中文，英文语言请输出英文。
                4. Markdown 正文结构要清晰，包含标题、小标题和段落。
                5. 根据文章类型采用合适写法：产品介绍可以写成通用方案介绍；教程强调步骤；行业科普强调知识解释；竞品对比强调客观比较；解决方案强调场景和路径；SEO文章强调搜索关键词覆盖。
                6. 返回 JSON 格式如下：
                {
                  "title": "...",
                  "summary": "...",
                  "content": "...",
                  "tags": ["标签1", "标签2"],
                  "keywords": ["关键词1", "关键词2"]
                }
                """.formatted(
                text(request.getTopic()),
                text(request.getType()),
                "ZH".equals(request.getLanguage()) ? "中文" : "英文",
                text(request.getExtraRequirement())
        );
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value : "未配置";
    }
}
