package com.aicontent.marketing.platformcontent.service;

import com.aicontent.marketing.article.entity.Article;
import com.aicontent.marketing.article.mapper.ArticleMapper;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.platformcontent.entity.ArticlePlatformContent;
import com.aicontent.marketing.platformcontent.mapper.ArticlePlatformContentMapper;
import com.aicontent.marketing.platformcontent.rule.PlatformAdaptRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticlePlatformContentServiceImplTest {

    @Mock
    private ArticlePlatformContentMapper baseMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private PlatformAdaptRules platformAdaptRules;

    private ArticlePlatformContentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticlePlatformContentServiceImpl(
                articleMapper, null, null, null, null, platformAdaptRules
        );
        // MyBatis-Plus ServiceImpl stores baseMapper internally
        ReflectionTestUtils.setField(service, "baseMapper", baseMapper);
    }

    // ==================== normalizePlatforms ====================

    @Test
    void normalizePlatformsRejectsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> invokeNormalizePlatforms(null));
        assertEquals("请选择生成平台", ex.getMessage());
    }

    @Test
    void normalizePlatformsRejectsEmptyList() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> invokeNormalizePlatforms(Collections.emptyList()));
        assertEquals("请选择生成平台", ex.getMessage());
    }

    @Test
    void normalizePlatformsRejectsUnsupportedPlatform() {
        when(platformAdaptRules.supports("INVALID")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> invokeNormalizePlatforms(List.of("INVALID")));
    }

    @Test
    void normalizePlatformsReturnsSupportedPlatforms() {
        when(platformAdaptRules.supports("WECHAT_OFFICIAL")).thenReturn(true);
        when(platformAdaptRules.supports("CSDN")).thenReturn(true);

        List<String> result = invokeNormalizePlatforms(List.of("WECHAT_OFFICIAL", "CSDN"));

        assertEquals(List.of("WECHAT_OFFICIAL", "CSDN"), result);
    }

    @Test
    void normalizePlatformsDeduplicates() {
        when(platformAdaptRules.supports("CSDN")).thenReturn(true);

        List<String> result = invokeNormalizePlatforms(List.of("CSDN", "CSDN"));

        assertEquals(List.of("CSDN"), result);
    }

    // ==================== requiredText ====================

    @Test
    void requiredTextReturnsValueWhenPresent() {
        String result = invokeRequiredText("标题", "标题为空");

        assertEquals("标题", result);
    }

    @Test
    void requiredTextThrowsWhenEmpty() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> invokeRequiredText("", "标题为空"));

        assertEquals("标题为空", ex.getMessage());
    }

    @Test
    void requiredTextThrowsWhenNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> invokeRequiredText(null, "标题为空"));

        assertEquals("标题为空", ex.getMessage());
    }

    // ==================== join ====================

    @Test
    void joinReturnsCommaSeparatedString() {
        String result = invokeJoin(List.of("AI工具", "内容营销"));

        assertEquals("AI工具,内容营销", result);
    }

    @Test
    void joinReturnsEmptyForNull() {
        assertEquals("", invokeJoin(null));
    }

    @Test
    void joinReturnsEmptyForEmptyList() {
        assertEquals("", invokeJoin(Collections.emptyList()));
    }

    // ==================== archive / restore ====================

    @Test
    void archiveSetsStatusToArchived() {
        ArticlePlatformContent content = new ArticlePlatformContent();
        content.setId(1L);
        content.setStatus("DRAFT");
        when(baseMapper.selectById(1L)).thenReturn(content);
        when(baseMapper.updateById(content)).thenReturn(1);

        service.archive(1L, 99L);

        assertEquals("ARCHIVED", content.getStatus());
        assertEquals(99L, content.getUpdatedBy());
        assertNotNull(content.getUpdatedAt());
    }

    @Test
    void restoreSetsStatusToDraft() {
        ArticlePlatformContent content = new ArticlePlatformContent();
        content.setId(1L);
        content.setStatus("ARCHIVED");
        when(baseMapper.selectById(1L)).thenReturn(content);
        when(baseMapper.updateById(content)).thenReturn(1);

        service.restore(1L, 99L);

        assertEquals("DRAFT", content.getStatus());
    }

    @Test
    void archiveThrowsWhenContentNotFound() {
        when(baseMapper.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.archive(1L, 99L));
    }

    // ==================== reflection helpers ====================

    @SuppressWarnings("unchecked")
    private List<String> invokeNormalizePlatforms(List<String> platforms) {
        return (List<String>) ReflectionTestUtils.invokeMethod(service,
                "normalizePlatforms", platforms);
    }

    private String invokeRequiredText(String value, String message) {
        return (String) ReflectionTestUtils.invokeMethod(service,
                "requiredText", value, message);
    }

    @SuppressWarnings("unchecked")
    private String invokeJoin(List<String> values) {
        return (String) ReflectionTestUtils.invokeMethod(service,
                "join", values);
    }
}
