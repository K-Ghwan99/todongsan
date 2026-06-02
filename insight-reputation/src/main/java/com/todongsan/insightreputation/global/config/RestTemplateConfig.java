package com.todongsan.insightreputation.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // JSON 컨버터가 text/html Content-Type도 처리하도록 설정
        // REB API 서버가 JSON 데이터를 text/html로 잘못 응답하는 문제 해결
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_HTML, // text/html도 JSON으로 파싱
            MediaType.TEXT_PLAIN // 혹시 모를 text/plain도 추가
        ));
        
        restTemplate.getMessageConverters().add(0, converter);
        return restTemplate;
    }
}