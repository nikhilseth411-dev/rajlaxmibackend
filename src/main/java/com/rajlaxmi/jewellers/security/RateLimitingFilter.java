package com.rajlaxmi.jewellers.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * RateLimitingFilter — IP-based request rate limiting via Redis
 *
 * Protects against:
 *   - Brute force login attempts (covered also by account locking)
 *   - API scraping / enumeration attacks
 *   - DDoS at application level
 *
 * STRATEGY: Sliding window counter per IP address.
 *   Key: rate_limit:{ip}:{endpoint_group}
 *   Value: request count in current window
 *   TTL: window duration (60 seconds)
 *
 * LIMITS (requests per 60 seconds):
 *   /auth/login       → 10 requests (brute force prevention)
 *   /auth/register    → 5 requests
 *   /auth/forgot*     → 5 requests
 *   All other /auth/* → 20 requests
 *   Other endpoints   → 100 requests (general API protection)
 *
 * On limit exceeded: HTTP 429 Too Many Requests.
 *
 * NOTE: In production with multiple app instances (horizontal scaling),
 * Redis ensures the counter is shared across all instances.
 * If Redis is unavailable, this filter gracefully allows all requests
 * rather than blocking the entire application (fail-open strategy).
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = extractClientIp(request);

        int limit = determineLimitForPath(path);
        String redisKey = "rate_limit:" + ip + ":" + getRateLimitGroup(path);

        try {
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Set expiry on first request in window
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS));
            }

            if (currentCount > limit) {
                log.warn("Rate limit exceeded for IP: {} on path: {} (count: {})", ip, path, currentCount);
                sendRateLimitResponse(response);
                return;
            }

            // Add rate limit headers for transparency
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));

        } catch (Exception e) {
            // Redis unavailable — fail open (allow request) rather than breaking the app
            log.warn("Rate limiting unavailable (Redis error): {}. Allowing request.", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private int determineLimitForPath(String path) {
        if (path.contains("/auth/login")) return 10;
        if (path.contains("/auth/register")) return 5;
        if (path.contains("/auth/forgot")) return 5;
        if (path.contains("/auth/")) return 20;
        return 100;
    }

    private String getRateLimitGroup(String path) {
        if (path.contains("/auth/login")) return "auth_login";
        if (path.contains("/auth/register")) return "auth_register";
        if (path.contains("/auth/")) return "auth_other";
        return "general";
    }

    /**
     * Extracts real client IP, accounting for reverse proxies (Nginx/Railway).
     * X-Forwarded-For header is set by proxies.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) return xRealIp;
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"success":false,"message":"Too many requests. Please wait before trying again.","timestamp":"%s"}
                """.formatted(java.time.LocalDateTime.now()));
    }
}
