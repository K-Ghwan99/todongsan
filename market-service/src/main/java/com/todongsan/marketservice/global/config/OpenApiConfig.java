package com.todongsan.marketservice.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI marketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Todongsan Market Service API")
                        .description("Pool Share 기반 즉시 참여형 예측시장 Market API")
                        .version("v1"));
    }
}
