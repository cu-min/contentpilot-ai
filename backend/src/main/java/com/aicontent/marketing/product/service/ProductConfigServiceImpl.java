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
import java.util.List;

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

        if (config == null) {
            return createConfig(request);
        }

        return updateConfig(config.getId(), request);
    }

    @Override
    public List<ProductConfigVO> listConfigs() {
        return list(new LambdaQueryWrapper<ProductConfig>()
                .orderByDesc(ProductConfig::getUpdatedAt)
                .orderByDesc(ProductConfig::getId))
                .stream()
                .map(ProductConfigVO::from)
                .toList();
    }

    @Override
    public ProductConfigVO getConfig(Long id) {
        ProductConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("产品配置不存在");
        }
        return ProductConfigVO.from(config);
    }

    @Override
    @Transactional
    public ProductConfigVO createConfig(ProductConfigSaveRequest request) {
        validateOfficialUrl(request.getOfficialUrl());
        ProductConfig config = new ProductConfig();
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        fillConfig(config, request, now);
        save(config);
        return ProductConfigVO.from(config);
    }

    @Override
    @Transactional
    public ProductConfigVO updateConfig(Long id, ProductConfigSaveRequest request) {
        validateOfficialUrl(request.getOfficialUrl());
        ProductConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("产品配置不存在");
        }
        fillConfig(config, request, LocalDateTime.now());
        updateById(config);
        return ProductConfigVO.from(config);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        if (!removeById(id)) {
            throw new BusinessException("产品配置不存在");
        }
    }

    private ProductConfig getFirstConfig() {
        return getOne(new LambdaQueryWrapper<ProductConfig>()
                .orderByAsc(ProductConfig::getId)
                .last("LIMIT 1"), false);
    }

    private void fillConfig(ProductConfig config, ProductConfigSaveRequest request, LocalDateTime updatedAt) {
        config.setProductName(request.getProductName());
        config.setProductIntro(request.getProductIntro());
        config.setOfficialUrl(request.getOfficialUrl());
        config.setCoreFeatures(request.getCoreFeatures());
        config.setTargetUsers(request.getTargetUsers());
        config.setAdvantages(request.getAdvantages());
        config.setBrandTone(request.getBrandTone());
        config.setBannedWords(request.getBannedWords());
        config.setUpdatedAt(updatedAt);
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
