package com.aicontent.marketing.publish.publisher;

import com.aicontent.marketing.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublisherRegistry {

    private static final String PLATFORM_JUEJIN = "JUEJIN";

    private final List<PlatformPublisher> publishers;
    private final MockPublisher mockPublisher;

    public PublisherRegistry(List<PlatformPublisher> publishers, MockPublisher mockPublisher) {
        this.publishers = publishers;
        this.mockPublisher = mockPublisher;
    }

    public PlatformPublisher getPublisher(String platform, String publishMode) {
        return publishers.stream()
                .filter(publisher -> exactMatch(publisher.platform(), platform) && exactMatch(publisher.mode(), publishMode))
                .findFirst()
                .orElseGet(() -> getFallbackPublisher(platform, publishMode));
    }

    private PlatformPublisher getFallbackPublisher(String platform, String publishMode) {
        if (PLATFORM_JUEJIN.equals(platform)) {
            throw new BusinessException("掘金真实 Publisher 未注册，不能回退到 MockPublisher");
        }
        return publishers.stream()
                .filter(publisher -> exactMatch(publisher.platform(), platform) && matches(publisher.mode(), publishMode))
                .findFirst()
                .or(() -> publishers.stream()
                        .filter(publisher -> publisher == mockPublisher)
                        .findFirst())
                .orElse(mockPublisher);
    }

    private boolean exactMatch(String supported, String actual) {
        return supported.equals(actual);
    }

    private boolean matches(String supported, String actual) {
        return "*".equals(supported) || supported.equals(actual);
    }
}
