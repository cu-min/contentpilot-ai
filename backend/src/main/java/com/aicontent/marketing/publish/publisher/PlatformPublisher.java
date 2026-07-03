package com.aicontent.marketing.publish.publisher;

public interface PlatformPublisher {

    String platform();

    String mode();

    PublishResult publish(PublishContext context);
}
