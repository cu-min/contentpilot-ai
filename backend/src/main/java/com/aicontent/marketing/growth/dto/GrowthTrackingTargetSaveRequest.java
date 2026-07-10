package com.aicontent.marketing.growth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GrowthTrackingTargetSaveRequest {

    @NotBlank(message = "请输入跟踪名称")
    @Size(max = 100, message = "跟踪名称最多 100 个字符")
    private String name;

    @NotBlank(message = "请选择平台")
    private String platform;

    @NotBlank(message = "请输入目标链接")
    @Size(max = 1000, message = "目标链接最多 1000 个字符")
    @Pattern(regexp = "(?i)^https?://.+$", message = "目标链接必须是 http 或 https URL")
    private String targetUrl;

    @Size(max = 500, message = "备注最多 500 个字符")
    private String remark;

    @NotNull(message = "请选择启用状态")
    private Integer enabled;
}
