package com.aicontent.marketing.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlatformAccountStatusRequest {

    @NotNull(message = "请选择启用状态")
    private Integer enabled;
}
