package com.aicontent.marketing.product.service;

import com.aicontent.marketing.product.dto.ProductConfigSaveRequest;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aicontent.marketing.product.entity.ProductConfig;

import java.util.List;

public interface ProductConfigService extends IService<ProductConfig> {

    ProductConfigVO getCurrentConfig();

    ProductConfigVO saveCurrentConfig(ProductConfigSaveRequest request);

    List<ProductConfigVO> listConfigs();

    ProductConfigVO getConfig(Long id);

    ProductConfigVO createConfig(ProductConfigSaveRequest request);

    ProductConfigVO updateConfig(Long id, ProductConfigSaveRequest request);

    void deleteConfig(Long id);

}
