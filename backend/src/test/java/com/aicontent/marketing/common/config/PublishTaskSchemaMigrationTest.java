package com.aicontent.marketing.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishTaskSchemaMigrationTest {

    @Test
    void migrateInvalidatesOnlyHistoricalMockPublishUrls() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);
        PublishTaskSchemaMigration migration = new PublishTaskSchemaMigration(jdbcTemplate);

        migration.migrate();

        verify(jdbcTemplate).update("""
                UPDATE platform_account
                SET default_publish_mode = 'BROWSER_AUTOMATION'
                WHERE platform IN ('CSDN', 'ZHIHU')
                  AND default_publish_mode = 'MANUAL_CONFIRM'
                """);
        verify(jdbcTemplate).update("""
                UPDATE publish_task
                SET publish_mode = 'BROWSER_AUTOMATION'
                WHERE platform IN ('CSDN', 'ZHIHU')
                  AND publish_mode = 'MANUAL_CONFIRM'
                  AND status IN ('DRAFT', 'PENDING', 'RUNNING')
                """);
        verify(jdbcTemplate).update("""
                UPDATE publish_task
                SET publish_url = NULL,
                    article_status = NULL,
                    status = 'FAILED',
                    error_message = '历史 Mock 结果已失效，请重新准备'
                WHERE publish_url LIKE 'https://mock.publish/%'
                """);
    }
}
