package com.aicontent.marketing.growth.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.growth.GrowthCheckStatus;
import com.aicontent.marketing.growth.dto.GrowthTrackingTargetQueryRequest;
import com.aicontent.marketing.growth.dto.GrowthTrackingTargetSaveRequest;
import com.aicontent.marketing.growth.entity.GrowthTrackingTarget;
import com.aicontent.marketing.growth.mapper.GrowthTrackingTargetMapper;
import com.aicontent.marketing.growth.vo.GrowthTrackingTargetVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GrowthTrackingTargetServiceImpl
        extends ServiceImpl<GrowthTrackingTargetMapper, GrowthTrackingTarget>
        implements GrowthTrackingTargetService {

    private static final Set<String> PLATFORMS = Set.of(
            "WECHAT_OFFICIAL", "ZHIHU", "CSDN", "JUEJIN", "OTHER"
    );
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title\\b[^>]*>(.*?)</title\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final int MAX_TITLE_LENGTH = 255;

    private final HttpClient httpClient;

    public GrowthTrackingTargetServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public List<GrowthTrackingTargetVO> listTargets(GrowthTrackingTargetQueryRequest request) {
        LambdaQueryWrapper<GrowthTrackingTarget> wrapper = new LambdaQueryWrapper<GrowthTrackingTarget>()
                .orderByDesc(GrowthTrackingTarget::getEnabled)
                .orderByDesc(GrowthTrackingTarget::getUpdatedAt)
                .orderByDesc(GrowthTrackingTarget::getId);
        if (StringUtils.hasText(request.getName())) {
            wrapper.like(GrowthTrackingTarget::getName, request.getName().trim());
        }
        if (StringUtils.hasText(request.getPlatform())) {
            validatePlatform(request.getPlatform());
            wrapper.eq(GrowthTrackingTarget::getPlatform, request.getPlatform());
        }
        if (request.getEnabled() != null) {
            validateEnabled(request.getEnabled());
            wrapper.eq(GrowthTrackingTarget::getEnabled, request.getEnabled());
        }
        return list(wrapper).stream().map(GrowthTrackingTargetVO::from).toList();
    }

    @Override
    @Transactional
    public GrowthTrackingTargetVO createTarget(GrowthTrackingTargetSaveRequest request, Long currentUserId) {
        validateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        GrowthTrackingTarget target = new GrowthTrackingTarget();
        fillTarget(target, request, now);
        target.setLastCheckStatus(GrowthCheckStatus.UNKNOWN.name());
        target.setCreatedBy(currentUserId);
        target.setCreatedAt(now);
        save(target);
        return GrowthTrackingTargetVO.from(target);
    }

    @Override
    @Transactional
    public GrowthTrackingTargetVO updateTarget(Long id, GrowthTrackingTargetSaveRequest request) {
        validateRequest(request);
        GrowthTrackingTarget target = getRequiredTarget(id);
        fillTarget(target, request, LocalDateTime.now());
        updateById(target);
        return GrowthTrackingTargetVO.from(target);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer enabled) {
        validateEnabled(enabled);
        GrowthTrackingTarget target = getRequiredTarget(id);
        target.setEnabled(enabled);
        target.setUpdatedAt(LocalDateTime.now());
        updateById(target);
    }

    @Override
    @Transactional
    public void deleteTarget(Long id) {
        if (!removeById(id)) {
            throw new BusinessException("跟踪目标不存在");
        }
    }

    @Override
    @Transactional
    public GrowthTrackingTargetVO checkTarget(Long id) {
        GrowthTrackingTarget target = getRequiredTarget(id);
        if (!Integer.valueOf(1).equals(target.getEnabled())) {
            throw new BusinessException("跟踪目标未启用，请先启用后再查询");
        }

        LocalDateTime checkedAt = LocalDateTime.now();
        try {
            URI targetUri = parseTargetUrl(target.getTargetUrl());
            HttpRequest request = HttpRequest.newBuilder(targetUri)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            target.setLastHttpStatus(statusCode);
            target.setLastPageTitle(extractTitle(response.body()));
            target.setLastErrorMessage(null);
            target.setLastCheckStatus(
                    statusCode >= 200 && statusCode <= 399
                            ? GrowthCheckStatus.SUCCESS.name()
                            : GrowthCheckStatus.FAILED.name()
            );
            if (statusCode >= 200 && statusCode <= 399) {
                target.setLastErrorMessage(null);
            } else {
                target.setLastErrorMessage("HTTP 状态码为 " + statusCode);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            recordFailure(target, exception);
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            recordFailure(target, exception);
        }
        target.setLastCheckedAt(checkedAt);
        target.setUpdatedAt(checkedAt);
        updateById(target);
        return GrowthTrackingTargetVO.from(target);
    }

    private void fillTarget(
            GrowthTrackingTarget target,
            GrowthTrackingTargetSaveRequest request,
            LocalDateTime updatedAt
    ) {
        target.setName(request.getName().trim());
        target.setPlatform(request.getPlatform());
        target.setTargetUrl(request.getTargetUrl().trim());
        target.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        target.setEnabled(request.getEnabled());
        target.setUpdatedAt(updatedAt);
    }

    private void validateRequest(GrowthTrackingTargetSaveRequest request) {
        validatePlatform(request.getPlatform());
        validateEnabled(request.getEnabled());
        parseTargetUrl(request.getTargetUrl());
    }

    private void validatePlatform(String platform) {
        if (!PLATFORMS.contains(platform)) {
            throw new BusinessException("platform is invalid");
        }
    }

    private void validateEnabled(Integer enabled) {
        if (!Integer.valueOf(0).equals(enabled) && !Integer.valueOf(1).equals(enabled)) {
            throw new BusinessException("enabled is invalid");
        }
    }

    private URI parseTargetUrl(String targetUrl) {
        try {
            URI uri = new URI(targetUrl == null ? "" : targetUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new BusinessException("targetUrl must be a valid http or https URL");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new BusinessException("targetUrl must be a valid http or https URL");
        }
    }

    private GrowthTrackingTarget getRequiredTarget(Long id) {
        GrowthTrackingTarget target = getById(id);
        if (target == null) {
            throw new BusinessException("跟踪目标不存在");
        }
        return target;
    }

    private void recordFailure(GrowthTrackingTarget target, Exception exception) {
        target.setLastCheckStatus(GrowthCheckStatus.FAILED.name());
        target.setLastHttpStatus(null);
        target.setLastPageTitle(null);
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            message = exception.getClass().getSimpleName();
        }
        target.setLastErrorMessage(truncate(message, MAX_ERROR_MESSAGE_LENGTH));
    }

    private String extractTitle(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        Matcher matcher = TITLE_PATTERN.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        String title = matcher.group(1).replaceAll("\\s+", " ").trim();
        return StringUtils.hasText(title) ? truncate(title, MAX_TITLE_LENGTH) : null;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
