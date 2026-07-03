package com.aicontent.marketing.publish.dto;

import lombok.Data;

@Data
public class PublishTaskQueryRequest {

    private long page = 1;

    private long size = 10;

    private String platform;

    private String status;

    private String publishType;
}
