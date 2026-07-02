package com.aicontent.marketing.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_config")
public class ProductConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private String productIntro;

    private String officialUrl;

    private String coreFeatures;

    private String targetUsers;

    private String advantages;

    private String brandTone;

    private String bannedWords;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
