package com.aicontent.marketing.publish.publisher.browser;

import com.aicontent.marketing.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrowserPublisherConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void legacyPublishFlagsAreIgnoredAndCannotChangeSafetyBoundary() {
        BrowserPublisherConfig.parse(
                config("https://editor.csdn.net/md/", "https://mp.csdn.net/mp_blog/manage/article", "/app/browser-data/csdn"),
                objectMapper,
                "CSDN",
                "https://editor.csdn.net/md/",
                "https://mp.csdn.net/mp_blog/manage/article",
                "/app/browser-data"
        );

        assertFalse(Arrays.stream(BrowserPublisherConfig.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("manualConfirm") || component.getName().equals("autoPublish")));
    }

    @Test
    void rejectsHttpOrWrongPlatformHosts() {
        assertThrows(BusinessException.class, () -> parseCsdn(config(
                "http://editor.csdn.net/md/",
                "https://mp.csdn.net/mp_blog/manage/article",
                "/app/browser-data/csdn"
        )));
        assertThrows(BusinessException.class, () -> parseCsdn(config(
                "https://evil.example/editor",
                "https://mp.csdn.net/mp_blog/manage/article",
                "/app/browser-data/csdn"
        )));
    }

    @Test
    void rejectsBrowserProfileOutsideAllowedRoot() {
        assertThrows(BusinessException.class, () -> parseCsdn(config(
                "https://editor.csdn.net/md/",
                "https://mp.csdn.net/mp_blog/manage/article",
                "/app/browser-data/../secrets"
        )));
    }

    private BrowserPublisherConfig parseCsdn(String rawConfig) {
        return BrowserPublisherConfig.parse(
                rawConfig,
                objectMapper,
                "CSDN",
                "https://editor.csdn.net/md/",
                "https://mp.csdn.net/mp_blog/manage/article",
                "/app/browser-data"
        );
    }

    private String config(String editorUrl, String manageUrl, String profilePath) {
        return """
                {
                  "browserUserDataDir": "%s",
                  "editorUrl": "%s",
                  "manageUrl": "%s",
                  "manualConfirm": false,
                  "autoPublish": true
                }
                """.formatted(profilePath, editorUrl, manageUrl);
    }
}
