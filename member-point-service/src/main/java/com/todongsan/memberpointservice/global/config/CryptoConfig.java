package com.todongsan.memberpointservice.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class CryptoConfig {

    @Value("${crypto.aes-key}")
    private String aesKey;

    @Bean
    public SecretKeySpec aesSecretKeySpec() {
        return new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
    }
}
