package com.aicontent.marketing.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlatformAccountSaveRequest {

    @NotBlank(message = "请选择平台")
    private String platform;

    @NotBlank(message = "请输入账号名称")
    @Size(max = 100, message = "账号名称最多 100 个字符")
    private String accountName;

    @NotBlank(message = "请选择认证方式")
    private String authType;

    private String authConfig;

    @NotBlank(message = "请选择默认发布方式")
    private String defaultPublishMode;

    @NotNull(message = "请选择启用状态")
    private Integer enabled;

    @Size(max = 500, message = "备注最多 500 个字符")
    private String remark;
}
