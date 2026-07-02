package com.rajlaxmi.jewellers.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * WebConfig — Static Resource Serving
 *
 * Exposes the uploads/ directory over HTTP so product images
 * can be accessed via: GET /uploads/products/image.jpg
 *
 * In production, serve images via NGINX or CDN instead,
 * but this works for Railway/Render deployment without extra infra.
 *
 * URL pattern: /uploads/** → file system: uploads/
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(toResourceLocation(uploadDir))
                .setCachePeriod(3600); // 1-hour client cache for images
    }

    static String toResourceLocation(String directory) {
        String location = Paths.get(directory).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
