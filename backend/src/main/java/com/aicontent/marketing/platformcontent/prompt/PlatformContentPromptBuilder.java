package com.aicontent.marketing.platformcontent.prompt;

import com.aicontent.marketing.article.entity.Article;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PlatformContentPromptBuilder {

    public String buildSystemPrompt() {
        return """
                你是一名专业的多平台内容运营编辑，擅长把一篇原始文章改写成适合不同内容平台发布的稿件。
                你必须严格按照目标平台调性改写，不要过度硬广，并保证输出可被后端程序稳定解析。
                输出要求：
                1. 只返回 JSON，不要返回 Markdown 代码块包裹。
                2. 不要返回额外解释文字。
                3. JSON 字段必须包含 title、summary、content、tags、keywords。
                4. tags 和 keywords 必须是字符串数组。
                5. content 字段内部必须是 Markdown 字符串。
                """;
    }

    public String buildUserPrompt(
            ProductConfigVO productConfig,
            Article article,
            String platform,
            String extraRequirement
    ) {
        return """
                请基于以下产品配置和原始文章，生成适合目标平台发布的内容稿。

                【目标平台】
                平台：%s
                平台写作风格：%s

                【产品配置】
                产品名称：%s
                产品简介：%s
                官网链接：%s
                核心功能：%s
                目标用户：%s
                产品优势：%s
                品牌语气：%s
                禁用词/敏感词：%s

                【原始文章】
                标题：%s
                摘要：%s
                类型：%s
                标签：%s
                关键词：%s
                正文：
                %s

                【额外要求】
                %s

                【改写规则】
                1. 保留原始文章的核心观点和事实，不编造产品不存在的能力。
                2. 根据目标平台调整标题、结构、语气和正文表达。
                3. 内容要自然、可信，不要过度硬广。
                4. 避免使用产品配置中的禁用词/敏感词。
                5. content 使用 Markdown，结构清晰，适合直接进入后续发布流程。
                6. 返回 JSON 格式如下：
                {
                  "title": "...",
                  "summary": "...",
                  "content": "...",
                  "tags": ["标签1", "标签2"],
                  "keywords": ["关键词1", "关键词2"]
                }
                """.formatted(
                platformLabel(platform),
                platformStyle(platform),
                text(productConfig.getProductName()),
                text(productConfig.getProductIntro()),
                text(productConfig.getOfficialUrl()),
                text(productConfig.getCoreFeatures()),
                text(productConfig.getTargetUsers()),
                text(productConfig.getAdvantages()),
                text(productConfig.getBrandTone()),
                text(productConfig.getBannedWords()),
                text(article.getTitle()),
                text(article.getSummary()),
                text(article.getType()),
                text(article.getTags()),
                text(article.getKeywords()),
                text(article.getContent()),
                text(extraRequirement)
        );
    }

    private String platformLabel(String platform) {
        return switch (platform) {
            case "WECHAT_OFFICIAL" -> "微信公众号";
            case "ZHIHU" -> "知乎";
            case "CSDN" -> "CSDN";
            case "JUEJIN" -> "掘金";
            default -> platform;
        };
    }

    private String platformStyle(String platform) {
        return switch (platform) {
            case "WECHAT_OFFICIAL" -> "完整、易读、有传播感，适合移动端阅读和品牌内容表达，标题要有吸引力但不过度标题党。";
            case "ZHIHU" -> "逻辑性强，观点明确，减少硬广感，更像问题分析、经验分享或方法论回答。";
            case "CSDN" -> "技术教程风格，结构清晰，步骤明确，适合开发者阅读，强调概念、流程和实践细节。";
            case "JUEJIN" -> "开发者视角，偏技术实践和经验总结，语气比 CSDN 更轻一点，表达更贴近日常开发经验。";
            default -> "保持专业内容营销文章风格，结构清晰、表达自然。";
        };
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value : "未配置";
    }
}
