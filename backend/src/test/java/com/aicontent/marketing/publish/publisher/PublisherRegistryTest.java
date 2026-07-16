package com.aicontent.marketing.publish.publisher;

import com.aicontent.marketing.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublisherRegistryTest {

    @Test
    void resolvesRegisteredPublisherWithoutRuntimeMockFallback() {
        PlatformPublisher publisher = mock(PlatformPublisher.class);
        when(publisher.platform()).thenReturn("CSDN");
        when(publisher.mode()).thenReturn("BROWSER_AUTOMATION");
        PublisherRegistry registry = new PublisherRegistry(List.of(publisher));

        assertSame(publisher, registry.getPublisher("CSDN", "BROWSER_AUTOMATION"));
    }

    @Test
    void missingPublisherFailsClosed() {
        PublisherRegistry registry = new PublisherRegistry(List.of());

        assertThrows(BusinessException.class, () -> registry.getPublisher("ZHIHU", "BROWSER_AUTOMATION"));
    }
}
