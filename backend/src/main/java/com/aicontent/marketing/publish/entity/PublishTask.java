package com.aicontent.marketing.publish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("publish_task")
public class PublishTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long platformContentId;

    private String platform;

    private Long accountId;

    private String title;

    private String status;

    private String publishType;

    private LocalDateTime scheduleTime;

    private String publishMode;

    private String publishUrl;

    private String errorMessage;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
