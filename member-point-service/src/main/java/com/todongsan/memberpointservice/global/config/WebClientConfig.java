package com.todongsan.memberpointservice.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// 카카오 API 호출용 WebClient
@Configuration
public class WebClientConfig {

    @Value("${kakao.api-base-url}")
    private String kakaoApiBaseUrl;

    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl(kakaoApiBaseUrl)
                .build();
    }
}
