package com.rajlaxmi.jewellers.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ================================================================
 * CorsConfig — Cross-Origin Resource Sharing
 * ================================================================
 * WHY CORS?
 *   The React frontend (e.g. localhost:5173 in dev, or
 *   rajlaxmi.vercel.app in prod) is a different "origin" than
 *   the Spring Boot API (localhost:8080 / api.rajlaxmi.com).
 *
 *   Browsers block cross-origin requests by default (Same-Origin Policy).
 *   CORS tells the browser "it's OK for these origins to call this API".
 *
 * ALLOWED ORIGINS:
 *   - localhost:5173 → Vite dev server
 *   - localhost:3000 → alternative dev port
 *   - Production domain (set via CORS_ALLOWED_ORIGINS env variable)
 *
 * ALLOWED METHODS:
 *   GET, POST, PUT, DELETE, PATCH — all REST methods our API uses.
 *
 * ALLOWED HEADERS:
 *   Authorization → needed for Bearer token
 *   Content-Type  → needed for JSON request bodies
 *
 * allowCredentials = true:
 *   Required if frontend sends cookies or Authorization headers.
 *   Must be combined with explicit origins (not wildcard "*").
 * ================================================================
 */
@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOriginsStr;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse comma-separated allowed origins from env variable
        List<String> allowedOrigins = Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        config.setExposedHeaders(List.of("Authorization")); // expose to frontend JS

        config.setAllowCredentials(true);

        config.setMaxAge(3600L); // pre-flight cache for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // apply to all endpoints

        return source;
    }
}
