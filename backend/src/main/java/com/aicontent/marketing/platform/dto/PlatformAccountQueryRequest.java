package com.aicontent.marketing.platform.dto;

import lombok.Data;

@Data
public class PlatformAccountQueryRequest {

    private String platform;

    private Integer enabled;
}
