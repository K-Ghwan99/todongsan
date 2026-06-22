package com.todongsan.insightreputation.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // REB API 서버가 JSON 데이터를 text/html로 잘못 응답하는 문제 해결
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_HTML,
            MediaType.TEXT_PLAIN
        ));

        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }

    // Claude API는 max_tokens=4000 응답 생성에 최대 2분 이상 걸릴 수 있으므로 별도 타임아웃 설정
    @Bean
    @Qualifier("claudeRestTemplate")
    public RestTemplate claudeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 연결 10초
        factory.setReadTimeout(120_000);     // 응답 대기 120초

        return new RestTemplate(factory);
    }
}