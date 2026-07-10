package com.aicontent.marketing.growth.vo;

import com.aicontent.marketing.growth.entity.GrowthTrackingTarget;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GrowthTrackingTargetVO {

    private Long id;

    private String name;

    private String platform;

    private String platformLabel;

    private String targetUrl;

    private String remark;

    private Integer enabled;

    private String lastCheckStatus;

    private Integer lastHttpStatus;

    private String lastPageTitle;

    private String lastErrorMessage;

    private LocalDateTime lastCheckedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static GrowthTrackingTargetVO from(GrowthTrackingTarget target) {
        GrowthTrackingTargetVO vo = new GrowthTrackingTargetVO();
        vo.setId(target.getId());
        vo.setName(target.getName());
        vo.setPlatform(target.getPlatform());
        vo.setPlatformLabel(platformLabel(target.getPlatform()));
        vo.setTargetUrl(target.getTargetUrl());
        vo.setRemark(target.getRemark());
        vo.setEnabled(target.getEnabled());
        vo.setLastCheckStatus(target.getLastCheckStatus());
        vo.setLastHttpStatus(target.getLastHttpStatus());
        vo.setLastPageTitle(target.getLastPageTitle());
        vo.setLastErrorMessage(target.getLastErrorMessage());
        vo.setLastCheckedAt(target.getLastCheckedAt());
        vo.setCreatedAt(target.getCreatedAt());
        vo.setUpdatedAt(target.getUpdatedAt());
        return vo;
    }

    private static String platformLabel(String platform) {
        return switch (platform) {
            case "WECHAT_OFFICIAL" -> "微信公众号";
            case "ZHIHU" -> "知乎";
            case "CSDN" -> "CSDN";
            case "JUEJIN" -> "掘金";
            case "OTHER" -> "其他";
            default -> platform == null ? "-" : platform;
        };
    }
}
