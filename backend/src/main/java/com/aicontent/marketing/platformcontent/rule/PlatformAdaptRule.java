package com.aicontent.marketing.platformcontent.rule;

import java.util.List;

public record PlatformAdaptRule(
        String platform,
        String platformName,
        String contentGoal,
        List<String> keepRules,
        List<String> removeRules,
        List<String> structureRules,
        List<String> toneRules,
        List<String> titleRules,
        List<String> tagRules,
        String promptDescription
) {
}
