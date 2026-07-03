package com.aicontent.marketing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("platform_account")
public class PlatformAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;

    private String accountName;

    private String authType;

    private String authConfig;

    private String defaultPublishMode;

    private Integer enabled;

    private String remark;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
