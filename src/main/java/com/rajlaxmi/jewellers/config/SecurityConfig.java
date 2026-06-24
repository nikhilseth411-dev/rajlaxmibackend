package com.rajlaxmi.jewellers.config;

import com.rajlaxmi.jewellers.security.CustomUserDetailsService;
import com.rajlaxmi.jewellers.security.JwtAuthenticationFilter;
import com.rajlaxmi.jewellers.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ================================================================
 * SecurityConfig — Core Spring Security Configuration
 * ================================================================
 * KEY DESIGN DECISIONS EXPLAINED:
 *
 * 1. STATELESS SESSION (SessionCreationPolicy.STATELESS)
 *    We use JWT, not server-side sessions.
 *    Spring Security won't create HttpSession objects.
 *    Every request is authenticated independently via JWT.
 *    This is required for REST APIs and horizontal scaling.
 *
 * 2. CSRF DISABLED
 *    CSRF attacks target session-cookie authentication.
 *    Since we use JWT in Authorization headers (not cookies),
 *    CSRF protection is unnecessary and disabled.
 *    RE-ENABLE if you ever switch to cookie-based auth.
 *
 * 3. @EnableMethodSecurity
 *    Enables @PreAuthorize, @PostAuthorize on controller methods.
 *    e.g. @PreAuthorize("hasRole('ADMIN')") on admin endpoints.
 *    More granular than URL-pattern authorization below.
 *
 * 4. PUBLIC ENDPOINTS — accessible without JWT:
 *    - /auth/** : login, register, forgot-password, verify-otp
 *    - GET /products/** : browse products without login
 *    - GET /categories/** : browse categories without login
 *    - GET /gold-rates/** : view gold prices without login
 *    - /actuator/health : health check for Railway/Render
 *    - /swagger-ui/** + /api-docs/** : API documentation
 *
 * 5. DaoAuthenticationProvider
 *    Connects Spring Security's auth system to our UserDetailsService
 *    and BCrypt password encoder. Used during login flow.
 * ================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // enables @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Main security filter chain configuration.
     * Defines which endpoints are public and which require authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> {})

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/gold-rates/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/silver-rates/**").permitAll()

                        // ADD THIS LINE
                        .requestMatchers(HttpMethod.GET, "/uploads/**", "/api/v1/uploads/**").permitAll()

                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                // FIXED FILTER ORDER
                .addFilterBefore(
                        rateLimitingFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * BCryptPasswordEncoder with strength 12.
     *
     * WHY strength 12?
     *   BCrypt strength controls number of rounds: 2^12 = 4096 iterations.
     *   Takes ~250ms to hash — slow enough to deter brute force,
     *   fast enough for production (login doesn't happen 1000x/sec).
     *   Default is 10; 12 is the recommended production value.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * DaoAuthenticationProvider:
     *   Loads UserDetails from DB via CustomUserDetailsService,
     *   then verifies the provided password against BCrypt hash.
     *   Used by AuthenticationManager during login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager:
     *   Delegates to the AuthenticationProvider above.
     *   Injected into AuthService to trigger authentication during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
