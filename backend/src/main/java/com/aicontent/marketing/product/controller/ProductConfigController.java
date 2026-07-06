package com.aicontent.marketing.product.controller;

import com.aicontent.marketing.common.result.Result;
import com.aicontent.marketing.product.dto.ProductConfigSaveRequest;
import com.aicontent.marketing.product.service.ProductConfigService;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductConfigController {

    private final ProductConfigService productConfigService;

    public ProductConfigController(ProductConfigService productConfigService) {
        this.productConfigService = productConfigService;
    }

    @GetMapping("/api/product-config")
    public Result<ProductConfigVO> getProductConfig() {
        return Result.success(productConfigService.getCurrentConfig());
    }

    @PutMapping("/api/product-config")
    public Result<ProductConfigVO> saveProductConfig(@Valid @RequestBody ProductConfigSaveRequest request) {
        return Result.success(productConfigService.saveCurrentConfig(request));
    }

    @GetMapping("/api/product-configs")
    public Result<List<ProductConfigVO>> listProductConfigs() {
        return Result.success(productConfigService.listConfigs());
    }

    @GetMapping("/api/product-configs/{id}")
    public Result<ProductConfigVO> getProductConfig(@PathVariable Long id) {
        return Result.success(productConfigService.getConfig(id));
    }

    @PostMapping("/api/product-configs")
    public Result<ProductConfigVO> createProductConfig(@Valid @RequestBody ProductConfigSaveRequest request) {
        return Result.success(productConfigService.createConfig(request));
    }

    @PutMapping("/api/product-configs/{id}")
    public Result<ProductConfigVO> updateProductConfig(
            @PathVariable Long id,
            @Valid @RequestBody ProductConfigSaveRequest request
    ) {
        return Result.success(productConfigService.updateConfig(id, request));
    }

    @DeleteMapping("/api/product-configs/{id}")
    public Result<Void> deleteProductConfig(@PathVariable Long id) {
        productConfigService.deleteConfig(id);
        return Result.success();
    }
}
