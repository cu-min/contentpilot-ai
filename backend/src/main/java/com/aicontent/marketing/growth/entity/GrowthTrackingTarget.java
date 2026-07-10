package com.aicontent.marketing.growth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_tracking_target")
public class GrowthTrackingTarget {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String platform;

    private String targetUrl;

    private String remark;

    private Integer enabled;

    private String lastCheckStatus;

    private Integer lastHttpStatus;

    private String lastPageTitle;

    private String lastErrorMessage;

    private LocalDateTime lastCheckedAt;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
