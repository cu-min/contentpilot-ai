package com.aicontent.marketing.product.controller;

import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.product.dto.ProductConfigSaveRequest;
import com.aicontent.marketing.product.service.ProductConfigService;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-config")
public class ProductConfigController {

    private final ProductConfigService productConfigService;

    public ProductConfigController(ProductConfigService productConfigService) {
        this.productConfigService = productConfigService;
    }

    @GetMapping
    public Result<ProductConfigVO> getProductConfig() {
        return Result.success(productConfigService.getCurrentConfig());
    }

    @PutMapping
    public Result<ProductConfigVO> saveProductConfig(@Valid @RequestBody ProductConfigSaveRequest request) {
        return Result.success(productConfigService.saveCurrentConfig(request));
    }
}
