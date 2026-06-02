package com.todongsan.insightreputation.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Insight-Reputation Service API")
                .description("동네대전 Insight-Reputation Service")
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList("X-Member-Id"))
            .components(new Components()
                .addSecuritySchemes("X-Member-Id",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Member-Id")));
    }
}