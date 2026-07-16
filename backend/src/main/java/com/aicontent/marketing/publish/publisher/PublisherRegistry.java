package com.aicontent.marketing.publish.publisher;

import com.aicontent.marketing.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublisherRegistry {

    private final List<PlatformPublisher> publishers;

    public PublisherRegistry(List<PlatformPublisher> publishers) {
        this.publishers = publishers;
    }

    public PlatformPublisher getPublisher(String platform, String publishMode) {
        return publishers.stream()
                .filter(publisher -> exactMatch(publisher.platform(), platform) && exactMatch(publisher.mode(), publishMode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "未注册对应的发布准备器：platform=" + platform + ", publishMode=" + publishMode
                ));
    }

    private boolean exactMatch(String supported, String actual) {
        return supported.equals(actual);
    }

}
