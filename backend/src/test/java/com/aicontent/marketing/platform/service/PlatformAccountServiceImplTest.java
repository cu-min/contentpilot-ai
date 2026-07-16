package com.aicontent.marketing.platform.service;

import com.aicontent.marketing.common.security.PlatformCredentialCipher;
import com.aicontent.marketing.common.exception.BusinessException;
import com.aicontent.marketing.platform.dto.PlatformAccountSaveRequest;
import com.aicontent.marketing.platform.entity.PlatformAccount;
import com.aicontent.marketing.publish.publisher.wechat.WechatAccessTokenCache;
import com.aicontent.marketing.publish.publisher.wechat.WechatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class PlatformAccountServiceImplTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String AUTH_CONFIG = """
            {"cookie":"sid=ok","defaultCategoryId":"category-1","defaultTagIds":["tag-1"]}
            """.trim();

    @Test
    void createEncryptsNewAuthConfigBeforePersistence() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(KEY);
        PlatformAccountServiceImpl service = spy(service(cipher));
        doReturn(true).when(service).save(any(PlatformAccount.class));

        service.createAccount(request(AUTH_CONFIG), 9L);

        ArgumentCaptor<PlatformAccount> captor = ArgumentCaptor.forClass(PlatformAccount.class);
        verify(service).save(captor.capture());
        String stored = captor.getValue().getAuthConfig();
        assertTrue(stored.startsWith("enc:v1:"));
        assertEquals(AUTH_CONFIG, cipher.decrypt(stored));
    }

    @Test
    void updatingLegacyAccountConvertsPlaintextWithoutBulkMigration() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(KEY);
        PlatformAccount existing = new PlatformAccount();
        existing.setId(1L);
        existing.setPlatform("JUEJIN");
        existing.setAuthConfig(AUTH_CONFIG);
        PlatformAccountServiceImpl service = spy(service(cipher));
        doReturn(existing).when(service).getById(1L);
        doReturn(true).when(service).updateById(existing);

        service.updateAccount(1L, request(null), 9L);

        assertTrue(existing.getAuthConfig().startsWith("enc:v1:"));
        assertEquals(AUTH_CONFIG, cipher.decrypt(existing.getAuthConfig()));
    }

    @Test
    void accountSaveRejectsUntrustedBrowserEditorHost() {
        PlatformAccountServiceImpl service = service(new PlatformCredentialCipher(KEY), "/app/browser-data");
        PlatformAccountSaveRequest request = new PlatformAccountSaveRequest();
        request.setPlatform("CSDN");
        request.setAccountName("unsafe account");
        request.setAuthType("BROWSER_PROFILE");
        request.setDefaultPublishMode("BROWSER_AUTOMATION");
        request.setEnabled(0);
        request.setAuthConfig("""
                {
                  "browserUserDataDir":"/app/browser-data/csdn",
                  "editorUrl":"https://evil.example/editor"
                }
                """);

        assertThrows(BusinessException.class, () -> service.createAccount(request, 9L));
    }

    private PlatformAccountServiceImpl service(PlatformCredentialCipher cipher) {
        return service(cipher, "");
    }

    private PlatformAccountServiceImpl service(PlatformCredentialCipher cipher, String allowedProfileRoot) {
        return new PlatformAccountServiceImpl(
                new ObjectMapper(),
                new WechatAccessTokenCache(),
                mock(WechatClient.class),
                cipher,
                allowedProfileRoot
        );
    }

    private PlatformAccountSaveRequest request(String authConfig) {
        PlatformAccountSaveRequest request = new PlatformAccountSaveRequest();
        request.setPlatform("JUEJIN");
        request.setAccountName("test account");
        request.setAuthType("COOKIE");
        request.setAuthConfig(authConfig);
        request.setDefaultPublishMode("UNOFFICIAL_API");
        request.setEnabled(0);
        return request;
    }
}
