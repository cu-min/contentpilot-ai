package com.aicontent.marketing.platformcontent.rule;

import com.aicontent.marketing.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PlatformAdaptRuleFactory {

    private final Map<String, PlatformAdaptRule> rules = Map.of(
            "WECHAT_OFFICIAL", new PlatformAdaptRule(
                    "WECHAT_OFFICIAL",
                    "微信公众号",
                    "让用户快速理解问题、产生兴趣，并愿意继续阅读或点击产品链接。",
                    List.of("核心痛点", "产品价值", "场景化案例", "清晰的小标题", "适度的品牌表达"),
                    List.of("过长技术细节", "大段代码", "复杂表格", "太多行业背景铺垫", "太硬的销售话术"),
                    List.of("标题更有吸引力", "开头要有场景或痛点", "段落短，适合手机阅读", "小标题清晰", "结尾可以有温和行动引导"),
                    List.of("专业", "易读", "有传播感", "不要像广告硬推"),
                    List.of("标题要有吸引力，但不要标题党", "可以突出痛点、结果或场景收益"),
                    List.of("标签偏品牌内容、用户场景、问题关键词", "关键词覆盖痛点和产品价值"),
                    "品牌内容、产品传播、移动端阅读。平台稿应像一篇适合手机阅读的品牌内容，不是技术长文或销售单页。"
            ),
            "ZHIHU", new PlatformAdaptRule(
                    "ZHIHU",
                    "知乎",
                    "让文章像是在回答一个真实问题，重点是逻辑、观点和可信度。",
                    List.of("问题背景", "逻辑推导", "对比分析", "真实痛点", "客观建议"),
                    List.of("明显营销口吻", "过多产品自夸", "口号式表达", "直接销售转化话术", "太短、太空的结论"),
                    List.of("标题可以是问题型或观点型", "开头先提出问题", "中间分点论证", "结尾给出判断或建议", "产品只能自然出现，不能强行推销"),
                    List.of("理性", "客观", "有观点", "减少硬广感"),
                    List.of("标题优先采用问题型或观点型", "标题要体现判断或讨论价值"),
                    List.of("标签偏问题、行业、方法论", "关键词应服务搜索和观点表达"),
                    "观点分析、问题回答、经验分享。平台稿应像一篇认真回答问题的内容，产品只作为自然方案出现。"
            ),
            "CSDN", new PlatformAdaptRule(
                    "CSDN",
                    "CSDN",
                    "让开发者觉得这篇内容有实操价值。",
                    List.of("技术背景", "操作步骤", "功能说明", "使用场景", "示例和配置说明", "可落地的方法"),
                    List.of("过多品牌故事", "运营化表达", "情绪化标题", "空泛价值描述", "与技术实践无关的段落"),
                    List.of("标题偏实用", "适合 Markdown", "可以有步骤编号", "可以有代码块或配置示例", "结构清晰，方便搜索"),
                    List.of("技术", "实用", "清晰", "像一篇开发者教程"),
                    List.of("标题突出教程、实践、配置、方案或问题解决", "避免夸张传播型标题"),
                    List.of("标签偏技术栈、教程、配置、实践场景", "关键词要方便开发者搜索"),
                    "技术教程、实践说明、开发者学习。平台稿应优先交付步骤、配置、示例和可落地方法。"
            ),
            "JUEJIN", new PlatformAdaptRule(
                    "JUEJIN",
                    "掘金",
                    "让内容像开发者实践总结，不要太官方。",
                    List.of("实践经验", "开发者痛点", "工具效率", "场景总结", "方法论"),
                    List.of("太官方的品牌宣传", "太重的企业营销口吻", "过长背景铺垫", "纯概念解释", "没有实践价值的段落"),
                    List.of("标题有技术价值感", "开头可以从开发者痛点切入", "正文偏经验总结", "语气比 CSDN 轻一点", "结尾可以有简短总结"),
                    List.of("开发者视角", "轻量", "实用", "经验分享感"),
                    List.of("标题要有技术价值感和实践感", "可以更贴近日常开发表达"),
                    List.of("标签偏开发实践、效率工具、经验总结", "关键词聚焦开发者痛点和方法"),
                    "开发者经验、技术实践、效率工具分享。平台稿应像开发者写给开发者的实践复盘。"
            )
    );

    public PlatformAdaptRule getRule(String platform) {
        PlatformAdaptRule rule = rules.get(platform);
        if (rule == null) {
            throw new BusinessException("platform is invalid");
        }
        return rule;
    }

    public boolean supports(String platform) {
        return rules.containsKey(platform);
    }

    public Set<String> supportedPlatforms() {
        return rules.keySet();
    }
}
