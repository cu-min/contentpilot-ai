package com.aicontent.marketing.publish.publisher;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublisherRegistry {

    private final List<PlatformPublisher> publishers;
    private final MockPublisher mockPublisher;

    public PublisherRegistry(List<PlatformPublisher> publishers, MockPublisher mockPublisher) {
        this.publishers = publishers;
        this.mockPublisher = mockPublisher;
    }

    public PlatformPublisher getPublisher(String platform, String publishMode) {
        return publishers.stream()
                .filter(publisher -> matches(publisher.platform(), platform) && matches(publisher.mode(), publishMode))
                .findFirst()
                .orElse(mockPublisher);
    }

    private boolean matches(String supported, String actual) {
        return "*".equals(supported) || supported.equals(actual);
    }
}
