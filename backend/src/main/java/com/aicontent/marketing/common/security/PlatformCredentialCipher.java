package com.aicontent.marketing.common.security;

import com.aicontent.marketing.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PlatformCredentialCipher {

    private static final String PREFIX = "enc:v1:";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public PlatformCredentialCipher(
            @Value("${platform.credentials.key:}") String encodedKey,
            @Value("${platform.credentials.required:false}") boolean required
    ) {
        if (required && !StringUtils.hasText(encodedKey)) {
            throw new IllegalStateException("PLATFORM_CREDENTIAL_KEY 在生产环境中必须配置");
        }
        this.key = decodeKey(encodedKey);
    }

    public PlatformCredentialCipher(String encodedKey) {
        this(encodedKey, false);
    }

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext) || isEncrypted(plaintext) || key == null) {
            return plaintext;
        }
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX
                    + Base64.getEncoder().encodeToString(nonce)
                    + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new BusinessException("平台认证配置加密失败");
        }
    }

    public String decrypt(String storedValue) {
        if (!StringUtils.hasText(storedValue) || !isEncrypted(storedValue)) {
            return storedValue;
        }
        if (key == null) {
            throw new BusinessException("平台认证配置已加密，但 PLATFORM_CREDENTIAL_KEY 未配置");
        }
        try {
            String[] parts = storedValue.split(":", -1);
            if (parts.length != 4 || !"enc".equals(parts[0]) || !"v1".equals(parts[1])) {
                throw new IllegalArgumentException("invalid encrypted credential format");
            }
            byte[] nonce = Base64.getDecoder().decode(parts[2]);
            byte[] encrypted = Base64.getDecoder().decode(parts[3]);
            if (nonce.length != NONCE_LENGTH_BYTES) {
                throw new IllegalArgumentException("invalid nonce length");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BusinessException("平台认证配置解密失败，请检查 PLATFORM_CREDENTIAL_KEY 或数据完整性");
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private SecretKeySpec decodeKey(String encodedKey) {
        if (!StringUtils.hasText(encodedKey)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
            if (decoded.length != KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException("invalid key length");
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("PLATFORM_CREDENTIAL_KEY 必须是 Base64 编码的 32 字节密钥", exception);
        }
    }
}
