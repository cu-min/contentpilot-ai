package com.aicontent.marketing.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductConfigSaveRequest {

    @NotBlank(message = "productName is required")
    @Size(max = 100, message = "productName must be less than 100 characters")
    private String productName;

    private String productIntro;

    @Size(max = 255, message = "officialUrl must be less than 255 characters")
    private String officialUrl;

    private String coreFeatures;

    private String targetUsers;

    private String advantages;

    @Size(max = 255, message = "brandTone must be less than 255 characters")
    private String brandTone;

    private String bannedWords;
}
