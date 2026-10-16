package com.example.Billing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI billNowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("XenBill API")
                        .version("1.0")
                        .description("Tenant-scoped billing API. Most operations require an HttpOnly JWT cookie and X-Branch-Id for the active shop.")
                        .contact(new Contact().name("XenBill Support").email("support@xenbill.app")))
                .components(new Components().addSecuritySchemes("cookieAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("jwt")
                                .description("HttpOnly access-token cookie issued by /api/auth/login or /api/auth/refresh")));
    }
}
