package com.alef33.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alef33OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ALEF 33 API")
                        .version("1.0")
                        .description("Customers, products, quotes, orders, invoices and payments. "
                                + "Log in with POST /api/auth/login, then click Authorize and paste the token."))
                // Adds the "Authorize" button in Swagger
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
