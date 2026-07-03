package com.aicontent.marketing.platformcontent.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PlatformContentGenerateRequest {

    @NotEmpty(message = "请选择生成平台")
    private List<String> platforms;

    private String extraRequirement;
}
