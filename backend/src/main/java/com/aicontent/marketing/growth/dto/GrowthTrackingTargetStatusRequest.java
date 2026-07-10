package com.aicontent.marketing.growth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrowthTrackingTargetStatusRequest {

    @NotNull(message = "请选择启用状态")
    private Integer enabled;
}
