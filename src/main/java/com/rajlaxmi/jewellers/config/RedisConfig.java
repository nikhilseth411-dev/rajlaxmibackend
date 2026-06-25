package com.rajlaxmi.jewellers.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * ================================================================
 * RedisConfig — Cache Configuration
 * ================================================================
 * WHAT WE CACHE:
 *   1. Gold prices — fetched hourly, cached for 1 hour
 *      Key: "gold_rates_current" → no DB hit for every homepage load
 *   2. Product listings — cached for 30 minutes
 *      Invalidated when admin updates a product
 *   3. Categories — cached for 2 hours (rarely change)
 *
 * WHY JSON SERIALIZATION instead of Java default serialization?
 *   - Human-readable in Redis CLI (debug friendly)
 *   - No ClassCastException if model classes change slightly
 *   - JavaTimeModule handles LocalDateTime serialization
 *
 * CACHE TTL STRATEGY:
 *   Short TTL = more DB queries but always fresh data
 *   Long TTL = fewer DB queries but potentially stale data
 *   For gold prices: exactly 1 hour (matches refresh interval)
 *   For products: 30 min (balance freshness vs performance)
 * ================================================================
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Keys stored as plain strings (readable in Redis CLI)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Values stored as JSON (readable + type-safe)
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // default: 30 min
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Custom TTLs per cache name
                .withCacheConfiguration("gold_rates",
                        defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("categories",
                        defaultConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("products",
                        defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .build();
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // handle LocalDateTime
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
