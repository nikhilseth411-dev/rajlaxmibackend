package com.rajlaxmi.jewellers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ================================================================
 * RajLaxmi Jewellers — Application Entry Point
 * ================================================================
 * Business : भगवान दास एंड संस — राज लक्ष्मी ज्वेलर्स
 * Location : Wazirganj, Gaya, Bihar — 805131
 *
 * Key Spring annotations used here:
 *
 * @SpringBootApplication
 *   → Combines @Configuration + @EnableAutoConfiguration + @ComponentScan
 *   → Boots the entire Spring context, auto-configures JPA, Security, Web, etc.
 *
 * @EnableCaching
 *   → Activates Spring's proxy-based caching abstraction
 *   → Enables @Cacheable, @CacheEvict annotations throughout the app
 *   → Backed by Redis (configured in application.yml)
 *   → Used for: gold prices, product listings, category data
 *
 * @EnableScheduling
 *   → Activates Spring's task scheduling framework
 *   → Enables @Scheduled annotations in scheduler classes
 *   → Used for: GoldPriceScheduler (runs every hour to fetch live rates)
 * ================================================================
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class RajlaxmiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RajlaxmiApplication.class, args);
    }
}
