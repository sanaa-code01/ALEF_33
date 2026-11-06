package com.alef33.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alef33OpenApi() {
        return new OpenAPI().info(new Info()
                .title("ALEF 33 API")
                .version("1.0")
                .description("Customers, products, quotes, orders, invoices and payments"));
    }
}