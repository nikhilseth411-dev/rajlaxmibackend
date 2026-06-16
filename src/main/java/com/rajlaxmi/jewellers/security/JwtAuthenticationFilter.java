package com.rajlaxmi.jewellers.security;

import com.rajlaxmi.jewellers.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ================================================================
 * JwtAuthenticationFilter — Runs Once Per HTTP Request
 * ================================================================
 * WHAT THIS FILTER DOES:
 *   Every API request passes through this filter.
 *   It extracts the JWT from the Authorization header,
 *   validates it, and sets the authenticated user in the
 *   Spring Security context.
 *
 * FLOW FOR AUTHENTICATED REQUESTS:
 *   1. Extract "Bearer <token>" from Authorization header
 *   2. Parse email from token payload
 *   3. Load UserDetails from DB using email
 *   4. Validate token (signature + expiry + email match)
 *   5. Set UsernamePasswordAuthenticationToken in SecurityContext
 *   6. Continue filter chain → controller receives authenticated request
 *
 * FLOW FOR UNAUTHENTICATED REQUESTS:
 *   - No Authorization header → skip this filter
 *   - Spring Security will deny access to protected endpoints
 *   - Public endpoints (/auth/**, /products/** etc.) pass through freely
 *
 * WHY extends OncePerRequestFilter?
 *   Guarantees this filter runs exactly once per request,
 *   even in forward/include scenarios within the servlet container.
 *
 * SecurityContextHolder:
 *   Thread-local storage for the current user's auth info.
 *   Cleared after each request by Spring Security automatically.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // ── Step 1: Extract Authorization header ──────────────
        final String authHeader = request.getHeader("Authorization");

        // If no Bearer token, skip this filter (public endpoint or missing auth)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract JWT token ─────────────────────────
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        final String jwt = authHeader.substring(7);

        // ── Step 3: Extract email from token ─────────────────
        String userEmail;
        try {
            userEmail = jwtUtil.extractEmail(jwt);
        } catch (Exception e) {
            log.debug("Could not extract email from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 4: Authenticate if not already authenticated ─
        // SecurityContextHolder.getContext().getAuthentication() == null means
        // this request hasn't been authenticated yet in this thread.
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load fresh UserDetails from DB — includes role, isActive, isLocked checks
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validate token signature, expiry, and email match
            if (jwtUtil.isTokenValid(jwt, userDetails)) {

                // Create Spring Security authentication token
                // credentials = null (we don't need password after JWT verification)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()  // ["ROLE_CUSTOMER"] or ["ROLE_ADMIN"]
                        );

                // Attach request details (IP address, session ID) to auth token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ✅ Set authenticated user in SecurityContext
                // From this point, @PreAuthorize checks can access this auth
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {} for request: {}", userEmail, request.getRequestURI());
            }
        }

        // ── Step 5: Continue the filter chain ─────────────────
        filterChain.doFilter(request, response);
    }
}
