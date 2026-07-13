package com.aicontent.marketing.product.service;

import com.aicontent.marketing.product.dto.ProductConfigSaveRequest;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.aicontent.marketing.product.entity.ProductConfig;

public interface ProductConfigService extends IService<ProductConfig> {

    ProductConfigVO getCurrentConfig();

    ProductConfigVO saveCurrentConfig(ProductConfigSaveRequest request);

}
