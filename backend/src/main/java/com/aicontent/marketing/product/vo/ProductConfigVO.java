package com.aicontent.marketing.product.vo;

import com.aicontent.marketing.product.entity.ProductConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductConfigVO {

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

    public static ProductConfigVO empty() {
        ProductConfigVO vo = new ProductConfigVO();
        vo.setProductName("");
        vo.setProductIntro("");
        vo.setOfficialUrl("");
        vo.setCoreFeatures("");
        vo.setTargetUsers("");
        vo.setAdvantages("");
        vo.setBrandTone("");
        vo.setBannedWords("");
        return vo;
    }

    public static ProductConfigVO from(ProductConfig config) {
        if (config == null) {
            return empty();
        }
        ProductConfigVO vo = new ProductConfigVO();
        vo.setId(config.getId());
        vo.setProductName(config.getProductName());
        vo.setProductIntro(config.getProductIntro());
        vo.setOfficialUrl(config.getOfficialUrl());
        vo.setCoreFeatures(config.getCoreFeatures());
        vo.setTargetUsers(config.getTargetUsers());
        vo.setAdvantages(config.getAdvantages());
        vo.setBrandTone(config.getBrandTone());
        vo.setBannedWords(config.getBannedWords());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }
}
