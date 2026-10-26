package com.tailfriends.auth.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 소셜 refresh token을 AES-256-GCM으로 암호화한다. 결과는 Base64(IV 12바이트 + 암호문 + 인증 태그)다.
 * 키는 환경 변수 OAUTH_TOKEN_ENCRYPTION_KEY(32바이트를 Base64로)로만 넣는다. 키를 바꾸면 저장된 토큰은 풀 수 없다.
 */
@Component
public class OAuthTokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 키가 비어 있으면 서버는 뜨지만 암호화할 때 실패한다(애플 로그인을 쓰지 않는 로컬 실행용).
     * 키 형식이 틀리면 서버를 띄우지 않는다.
     */
    public OAuthTokenCipher(@Value("${oauth.token-encryption-key:}") String base64Key) {
        this.key = StringUtils.hasText(base64Key) ? toKey(base64Key) : null;
    }

    public String encrypt(String plainText) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰을 암호화하지 못했어요", e);
        }
    }

    /**
     * @throws IllegalStateException 키가 다르거나 값이 바뀌어 풀 수 없을 때
     */
    public String decrypt(String cipherText) {
        try {
            byte[] bytes = Base64.getDecoder().decode(cipherText);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("토큰을 복호화하지 못했어요", e);
        }
    }

    private SecretKey requireKey() {
        if (key == null) {
            throw new IllegalStateException("토큰 암호화 키(OAUTH_TOKEN_ENCRYPTION_KEY)가 설정되지 않았어요");
        }
        return key;
    }

    private static SecretKey toKey(String base64Key) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("OAUTH_TOKEN_ENCRYPTION_KEY는 Base64여야 해요", e);
        }
        if (bytes.length != KEY_BYTES) {
            throw new IllegalStateException("OAUTH_TOKEN_ENCRYPTION_KEY는 32바이트(Base64 44자)여야 해요");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
