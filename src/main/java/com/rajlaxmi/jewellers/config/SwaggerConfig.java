package com.rajlaxmi.jewellers.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ================================================================
 * SwaggerConfig — OpenAPI 3 Documentation
 * ================================================================
 * Accessible at: http://localhost:8080/api/v1/swagger-ui.html
 *
 * JWT in Swagger:
 *   The BearerAuth security scheme lets you click "Authorize"
 *   in Swagger UI and enter your JWT token once.
 *   All subsequent API calls in the UI will include the token.
 *
 * HOW TO TEST with Swagger:
 *   1. Call POST /auth/login with email + password
 *   2. Copy the accessToken from the response
 *   3. Click "Authorize" button → enter: Bearer <your_token>
 *   4. Now all protected endpoints work in Swagger UI
 * ================================================================
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI rajlaxmiOpenAPI() {
        String apiBasePath = (contextPath == null || contextPath.isBlank()) ? "/" : contextPath;

        return new OpenAPI()
                .info(new Info()
                        .title("🪙 राज लक्ष्मी ज्वेलर्स — REST API")
                        .description("""
                                **भगवान दास एंड संस — राज लक्ष्मी ज्वेलर्स**
                                
                                Production-ready jewellery e-commerce API.
                                
                                **Business:** Wazirganj, Gaya, Bihar — 805131
                                
                                **Authentication:** JWT Bearer Token
                                - Login via POST /auth/login
                                - Use the returned accessToken in Authorize button above
                                - Token expires in 15 minutes
                                - Use POST /auth/refresh to get a new token
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("RajLaxmi Support")
                                .email("rajlaxmijewellers.gaya@gmail.com")
                        )
                )
                .servers(List.of(
                        new Server().url(apiBasePath).description("Current API base path")
                ))
                // ── JWT Security Scheme ──────────────────────
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token from /auth/login response")
                        )
                );
    }
}
