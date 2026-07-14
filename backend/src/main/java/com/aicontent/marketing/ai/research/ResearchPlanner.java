package com.aicontent.marketing.ai.research;

import com.aicontent.marketing.ai.dto.AiArticleGenerateRequest;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResearchPlanner {

    private static final String[] FRESHNESS_TERMS = {
            "最新", "今日", "今天", "本周", "近期", "新闻", "政策", "价格", "活动",
            "latest", "today", "this week", "news", "policy", "price", "event"
    };
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-/.年](\\d{1,2})(?:[-/.月](\\d{1,2})日?)?");
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    public ResearchPlan plan(ProductConfigVO productConfig, AiArticleGenerateRequest request) {
        ResearchPlan plan = new ResearchPlan();
        plan.setOfficialUrl(productConfig == null ? null : normalizeUrl(productConfig.getOfficialUrl()));
        plan.setQuery(buildQuery(productConfig, request));
        DateRange explicitRange = findExplicitRange(request);
        if (explicitRange != null) {
            plan.setStartPublishedDate(explicitRange.start());
            plan.setEndPublishedDate(explicitRange.end());
        } else if (requiresFreshness(request)) {
            plan.setStartPublishedDate(Instant.now().minus(7, ChronoUnit.DAYS));
        }
        return plan;
    }

    private String buildQuery(ProductConfigVO productConfig, AiArticleGenerateRequest request) {
        StringBuilder query = new StringBuilder("为内容营销文章搜集可靠资料。主题：")
                .append(text(request.getTopic()))
                .append("。文章类型：")
                .append(text(request.getType()))
                .append("。额外要求：")
                .append(text(request.getExtraRequirement()));
        if (productConfig != null) {
            query.append("。产品：").append(text(productConfig.getProductName()))
                    .append("。核心功能：").append(text(productConfig.getCoreFeatures()))
                    .append("。目标用户：").append(text(productConfig.getTargetUsers()))
                    .append("。产品优势：").append(text(productConfig.getAdvantages()));
        }
        return query.toString();
    }

    private boolean requiresFreshness(AiArticleGenerateRequest request) {
        String content = (text(request.getTopic()) + " " + text(request.getExtraRequirement()))
                .toLowerCase(Locale.ROOT);
        for (String term : FRESHNESS_TERMS) {
            if (content.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private DateRange findExplicitRange(AiArticleGenerateRequest request) {
        String content = text(request.getTopic()) + " " + text(request.getExtraRequirement());
        Matcher matcher = DATE_PATTERN.matcher(content);
        List<LocalDate> dates = new ArrayList<>();
        List<Boolean> monthOnly = new ArrayList<>();
        while (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                String day = matcher.group(3);
                dates.add(LocalDate.of(year, month, day == null ? 1 : Integer.parseInt(day)));
                monthOnly.add(day == null);
            } catch (RuntimeException ignored) {
                // Invalid date fragments are normal prose, not a search failure.
            }
        }
        if (dates.isEmpty()) {
            return null;
        }
        LocalDate start = dates.get(0);
        LocalDate end = dates.size() > 1 ? dates.get(1) : start;
        boolean endIsMonthOnly = dates.size() > 1 ? monthOnly.get(1) : monthOnly.get(0);
        if (endIsMonthOnly) {
            end = end.with(TemporalAdjusters.lastDayOfMonth());
        }
        if (end.isBefore(start)) {
            return null;
        }
        return new DateRange(start.atStartOfDay(SHANGHAI).toInstant(),
                end.plusDays(1).atStartOfDay(SHANGHAI).toInstant());
    }

    private String normalizeUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && StringUtils.hasText(uri.getHost()) ? uri.toString() : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : "未提供";
    }

    private record DateRange(Instant start, Instant end) {
    }
}
