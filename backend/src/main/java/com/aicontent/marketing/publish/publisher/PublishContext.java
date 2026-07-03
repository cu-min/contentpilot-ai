package com.aicontent.marketing.publish.publisher;

import java.time.LocalDateTime;

public record PublishContext(
        Long taskId,
        Long articleId,
        Long platformContentId,
        String platform,
        Long accountId,
        String title,
        String content,
        String summary,
        String tags,
        String keywords,
        String publishMode,
        boolean accountConfigExists,
        String accountAuthConfig,
        String accountName,
        String accountRemark,
        LocalDateTime scheduleTime
) {
}
