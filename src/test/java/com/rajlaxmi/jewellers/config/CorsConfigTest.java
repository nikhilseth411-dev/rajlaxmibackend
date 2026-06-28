package com.rajlaxmi.jewellers.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void alwaysAllowsProductionFrontendOrigins() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(
                config,
                "allowedOriginsStr",
                "http://localhost:5173"
        );

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/auth/login")
        );

        assertNotNull(cors);
        assertNotNull(cors.getAllowedOrigins());
        assertTrue(cors.getAllowedOrigins().contains("https://rajlaxmijewellery.in"));
        assertTrue(cors.getAllowedOrigins().contains("https://www.rajlaxmijewellery.in"));
        assertTrue(cors.getAllowedOrigins().contains("https://rajlaxmi-frontend.vercel.app"));
    }
}
