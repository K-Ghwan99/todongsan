package com.todongsan.memberpointservice.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoUtilTest {

    private CryptoUtil cryptoUtil;

    @BeforeEach
    void setUp() {
        String key = "test-aes-key-must-be-32-chars!!!";
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "AES");
        cryptoUtil = new CryptoUtil(secretKeySpec);
    }

    @Test
    void encrypt_decrypt_라운드트립_성공() {
        String plaintext = "test-kakao-access-token";

        String encrypted = cryptoUtil.encrypt(plaintext);
        String decrypted = cryptoUtil.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_같은_입력_다른_암호문() {
        // IV가 매번 달라서 동일 입력도 다른 암호문 생성
        String plaintext = "test-token";

        String enc1 = cryptoUtil.encrypt(plaintext);
        String enc2 = cryptoUtil.encrypt(plaintext);

        assertThat(enc1).isNotEqualTo(enc2);
    }
}
