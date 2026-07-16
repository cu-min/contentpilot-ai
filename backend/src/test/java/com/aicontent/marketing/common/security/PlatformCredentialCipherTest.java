package com.aicontent.marketing.common.security;

import com.aicontent.marketing.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformCredentialCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String PLAINTEXT = "{\"cookie\":\"sensitive-value\"}";

    @Test
    void encryptsAndDecryptsWithVersionedAesGcmFormat() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(KEY);

        String encrypted = cipher.encrypt(PLAINTEXT);

        assertTrue(encrypted.startsWith("enc:v1:"));
        assertNotEquals(PLAINTEXT, encrypted);
        assertEquals(PLAINTEXT, cipher.decrypt(encrypted));
    }

    @Test
    void usesRandomNonceForEveryEncryption() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(KEY);

        assertNotEquals(cipher.encrypt(PLAINTEXT), cipher.encrypt(PLAINTEXT));
    }

    @Test
    void rejectsTamperedCiphertext() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher(KEY);
        String encrypted = cipher.encrypt(PLAINTEXT);
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";

        assertThrows(BusinessException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void readsLegacyPlaintextWithoutKey() {
        PlatformCredentialCipher cipher = new PlatformCredentialCipher("");

        assertEquals(PLAINTEXT, cipher.decrypt(PLAINTEXT));
        assertEquals(PLAINTEXT, cipher.encrypt(PLAINTEXT));
    }

    @Test
    void encryptedValueWithoutKeyFailsInsteadOfFallingBackToPlaintext() {
        String encrypted = new PlatformCredentialCipher(KEY).encrypt(PLAINTEXT);
        PlatformCredentialCipher cipherWithoutKey = new PlatformCredentialCipher("");

        BusinessException exception = assertThrows(BusinessException.class, () -> cipherWithoutKey.decrypt(encrypted));
        assertTrue(exception.getMessage().contains("PLATFORM_CREDENTIAL_KEY"));
    }

    @Test
    void productionModeRejectsMissingKey() {
        assertThrows(IllegalStateException.class, () -> new PlatformCredentialCipher("", true));
    }
}
