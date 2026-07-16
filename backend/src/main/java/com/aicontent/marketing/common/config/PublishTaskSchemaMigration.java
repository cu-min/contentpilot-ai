package com.aicontent.marketing.common.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class PublishTaskSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    public PublishTaskSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        addColumnIfMissing("external_draft_id", "VARCHAR(100)");
        addColumnIfMissing("external_publish_id", "VARCHAR(255)");
        addColumnIfMissing("external_article_id", "VARCHAR(100)");
        addColumnIfMissing("draft_url", "VARCHAR(500)");
        addColumnIfMissing("article_status", "VARCHAR(50)");
        addIndexIfMissing("idx_publish_task_scheduled_due", "publish_type, status, schedule_time");
        normalizeBrowserPreparationMode();
        invalidateHistoricalMockResults();
    }

    private void addColumnIfMissing(String columnName, String columnDefinition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'publish_task'
                  AND column_name = ?
                """,
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE publish_task ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private void addIndexIfMissing(String indexName, String columns) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'publish_task'
                  AND index_name = ?
                """,
                Integer.class,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE publish_task ADD INDEX " + indexName + " (" + columns + ")");
        }
    }

    private void invalidateHistoricalMockResults() {
        jdbcTemplate.update("""
                UPDATE publish_task
                SET publish_url = NULL,
                    article_status = NULL,
                    status = 'FAILED',
                    error_message = '历史 Mock 结果已失效，请重新准备'
                WHERE publish_url LIKE 'https://mock.publish/%'
                """);
    }

    private void normalizeBrowserPreparationMode() {
        jdbcTemplate.update("""
                UPDATE platform_account
                SET default_publish_mode = 'BROWSER_AUTOMATION'
                WHERE platform IN ('CSDN', 'ZHIHU')
                  AND default_publish_mode = 'MANUAL_CONFIRM'
                """);
        jdbcTemplate.update("""
                UPDATE publish_task
                SET publish_mode = 'BROWSER_AUTOMATION'
                WHERE platform IN ('CSDN', 'ZHIHU')
                  AND publish_mode = 'MANUAL_CONFIRM'
                  AND status IN ('DRAFT', 'PENDING', 'RUNNING')
                """);
    }
}
