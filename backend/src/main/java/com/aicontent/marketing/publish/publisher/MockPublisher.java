package com.aicontent.marketing.publish.publisher;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class MockPublisher implements PlatformPublisher {

    private static final String MOCK_FAIL = "mock-fail";

    @Override
    public String platform() {
        return "*";
    }

    @Override
    public String mode() {
        return "*";
    }

    @Override
    public PublishResult publish(PublishContext context) {
        if (containsMockFail(context)) {
            return PublishResult.failed("MockPublisher 模拟发布失败：检测到 mock-fail");
        }
        return PublishResult.success(
                "https://mock.publish/%s/%s".formatted(context.platform(), context.taskId()),
                "MockPublisher 模拟发布成功"
        );
    }

    private boolean containsMockFail(PublishContext context) {
        return contains(context.title())
                || contains(context.summary())
                || contains(context.content())
                || contains(context.tags())
                || contains(context.keywords())
                || contains(context.accountRemark());
    }

    private boolean contains(String value) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(MOCK_FAIL);
    }
}
