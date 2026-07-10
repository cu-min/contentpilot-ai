package com.aicontent.marketing.growth.dto;

import lombok.Data;

@Data
public class GrowthTrackingTargetQueryRequest {

    private String name;

    private String platform;

    private Integer enabled;
}
