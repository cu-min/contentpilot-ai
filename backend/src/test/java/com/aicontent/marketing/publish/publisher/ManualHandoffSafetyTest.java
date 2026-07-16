package com.aicontent.marketing.publish.publisher;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualHandoffSafetyTest {

    @Test
    void productionPublishersContainNoFinalSubmissionCall() throws IOException {
        String wechat = source("wechat/WechatOfficialApiPublisher.java");
        String juejin = source("juejin/JuejinPublisher.java");
        String csdn = source("csdn/CsdnBrowserPublisher.java");
        String zhihu = source("zhihu/ZhihuBrowserPublisher.java");

        assertFalse(wechat.contains("submitFreePublish("));
        assertFalse(juejin.contains("publishArticle("));
        assertFalse(csdn.contains("clickFinalPublishButton("));
        assertFalse(zhihu.contains("clickFinalPublishButton("));
        assertTrue(wechat.contains("PublishResult.prepared("));
        assertTrue(juejin.contains("PublishResult.prepared("));
        assertTrue(csdn.contains("finalPublishButtonVisible("));
        assertTrue(zhihu.contains("publishButtonVisible("));
        assertFalse(zhihu.contains("clickPublishButton("));
        assertFalse(zhihu.contains("PublishResult.success("));
        assertFalse(csdn.contains("PublishResult.success("));
    }

    private String source(String relativePath) throws IOException {
        return Files.readString(Path.of(
                "src/main/java/com/aicontent/marketing/publish/publisher",
                relativePath
        ));
    }
}
