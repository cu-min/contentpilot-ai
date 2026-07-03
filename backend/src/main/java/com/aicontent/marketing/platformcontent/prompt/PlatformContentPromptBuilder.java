package com.aicontent.marketing.platformcontent.prompt;

import com.aicontent.marketing.article.entity.Article;
import com.aicontent.marketing.platformcontent.rule.PlatformAdaptRule;
import com.aicontent.marketing.platformcontent.rule.PlatformAdaptRuleFactory;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class PlatformContentPromptBuilder {

    private final PlatformAdaptRuleFactory ruleFactory;

    public PlatformContentPromptBuilder(PlatformAdaptRuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
    }

    public String buildSystemPrompt() {
        return """
                你是一名专业的多平台内容策略编辑，擅长把一篇原始文章重组为适合不同内容平台发布的稿件。
                平台适配不是扩写，也不是简单改写。你需要先理解原文核心观点，再根据目标平台进行删减、重组和改写。
                你不需要保留原文所有段落，可以删除不适合目标平台的背景、铺垫、技术细节、硬广表达和重复内容。
                你可以调整文章标题、结构、段落顺序和表达方式，但必须保持内容真实，不能编造产品功能。
                你必须严格规避产品配置中的禁用词/敏感词，并保证输出可被后端程序稳定解析。
                输出要求：
                1. 只返回 JSON，不要返回 Markdown 代码块包裹。
                2. 不要返回额外解释文字。
                3. JSON 字段必须包含 title、summary、content、tags、keywords。
                4. tags 和 keywords 必须是字符串数组。
                5. content 字段内部必须是 Markdown 字符串。
                6. 能减则减，删除不服务平台阅读目标的内容。
                """;
    }

    public String buildUserPrompt(
            ProductConfigVO productConfig,
            Article article,
            String platform,
            String extraRequirement
    ) {
        PlatformAdaptRule rule = ruleFactory.getRule(platform);
        return """
                请基于以下产品配置和原始文章，生成适合目标平台发布的内容稿。
                注意：本任务是平台化内容重组，不是扩写，也不是逐段复制原文。

                【目标平台】
                平台：%s
                平台定位：%s
                内容目标：%s

                【平台适配规则】
                保留规则：
                %s

                删减规则：
                %s

                结构规则：
                %s

                语气规则：
                %s

                标题规则：
                %s

                标签/关键词规则：
                %s

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

                【重组步骤】
                1. 先提炼原文核心观点、产品价值、用户痛点、解决方案和转化引导。
                2. 对照目标平台规则，删除不适合当前平台的内容；不需要保留原文所有段落。
                3. 重新组织标题、开头、正文段落、小标题和结尾，让文章符合目标平台阅读习惯。
                4. 能减则减，删除重复背景、过长铺垫、过强硬广、不必要技术细节和复杂长段落。
                5. 保留事实准确性，不编造产品功能、案例、数据或平台能力。
                6. 禁止使用产品配置中的禁用词/敏感词。
                7. content 使用 Markdown，结构清晰，适合直接进入后续发布流程。
                8. 返回 JSON 格式如下：
                {
                  "title": "...",
                  "summary": "...",
                  "content": "...",
                  "tags": ["标签1", "标签2"],
                  "keywords": ["关键词1", "关键词2"]
                }
                """.formatted(
                rule.platformName(),
                rule.promptDescription(),
                rule.contentGoal(),
                formatRules(rule.keepRules()),
                formatRules(rule.removeRules()),
                formatRules(rule.structureRules()),
                formatRules(rule.toneRules()),
                formatRules(rule.titleRules()),
                formatRules(rule.tagRules()),
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

    private String formatRules(List<String> rules) {
        return rules.stream()
                .map(rule -> "- " + rule)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- 未配置");
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value : "未配置";
    }
}
