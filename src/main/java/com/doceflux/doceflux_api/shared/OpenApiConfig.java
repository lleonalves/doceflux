package com.doceflux.doceflux_api.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI doceFluxOpenApi() {
        return new OpenAPI().info(new Info()
                .title("DoceFlux API")
                .description("API REST para o sistema de gestão de confeitaria DoceFlux")
                .version("1.0.0"));
    }
}
