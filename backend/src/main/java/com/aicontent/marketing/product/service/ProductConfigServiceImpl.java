package com.aicontent.marketing.product.service;

import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.product.dto.ProductConfigSaveRequest;
import com.aicontent.marketing.product.entity.ProductConfig;
import com.aicontent.marketing.product.mapper.ProductConfigMapper;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;

@Service
public class ProductConfigServiceImpl extends ServiceImpl<ProductConfigMapper, ProductConfig> implements ProductConfigService {

    @Override
    public ProductConfigVO getCurrentConfig() {
        return ProductConfigVO.from(getFirstConfig());
    }

    @Override
    @Transactional
    public ProductConfigVO saveCurrentConfig(ProductConfigSaveRequest request) {
        validateOfficialUrl(request.getOfficialUrl());
        ProductConfig config = getFirstConfig();
        LocalDateTime now = LocalDateTime.now();

        if (config == null) {
            config = new ProductConfig();
            config.setCreatedAt(now);
        }

        config.setProductName(request.getProductName());
        config.setProductIntro(request.getProductIntro());
        config.setOfficialUrl(request.getOfficialUrl());
        config.setCoreFeatures(request.getCoreFeatures());
        config.setTargetUsers(request.getTargetUsers());
        config.setAdvantages(request.getAdvantages());
        config.setBrandTone(request.getBrandTone());
        config.setBannedWords(request.getBannedWords());
        config.setUpdatedAt(now);

        saveOrUpdate(config);
        return ProductConfigVO.from(config);
    }

    private ProductConfig getFirstConfig() {
        return getOne(new LambdaQueryWrapper<ProductConfig>()
                .orderByAsc(ProductConfig::getId)
                .last("LIMIT 1"), false);
    }

    private void validateOfficialUrl(String officialUrl) {
        if (!StringUtils.hasText(officialUrl)) {
            return;
        }
        try {
            URI uri = new URI(officialUrl);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new BusinessException("officialUrl must be a valid http or https URL");
            }
        } catch (URISyntaxException exception) {
            throw new BusinessException("officialUrl must be a valid http or https URL");
        }
    }
}
