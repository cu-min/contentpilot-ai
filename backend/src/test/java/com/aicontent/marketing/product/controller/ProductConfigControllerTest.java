package com.aicontent.marketing.product.controller;

import com.aicontent.marketing.product.service.ProductConfigService;
import com.aicontent.marketing.product.vo.ProductConfigVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductConfigControllerTest {

    @Test
    void listsEveryConfiguredProductInsteadOfAFirstProductOnlyView() {
        ProductConfigService service = mock(ProductConfigService.class);
        ProductConfigVO first = new ProductConfigVO();
        first.setId(1L);
        ProductConfigVO second = new ProductConfigVO();
        second.setId(2L);
        when(service.listConfigs()).thenReturn(List.of(first, second));
        ProductConfigController controller = new ProductConfigController(service);

        List<ProductConfigVO> result = controller.listProductConfigs().getData();

        assertEquals(List.of(first, second), result);
        verify(service).listConfigs();
    }
}
