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
        addColumnIfMissing("external_article_id", "VARCHAR(100)");
        addColumnIfMissing("draft_url", "VARCHAR(500)");
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
}
