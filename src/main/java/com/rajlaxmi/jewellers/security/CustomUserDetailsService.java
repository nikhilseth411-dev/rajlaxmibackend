package com.rajlaxmi.jewellers.security;

import com.rajlaxmi.jewellers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ================================================================
 * CustomUserDetailsService
 * ================================================================
 * Spring Security calls loadUserByUsername() during:
 *   1. Login (UsernamePasswordAuthenticationToken)
 *   2. Every JWT-authenticated request (JwtAuthenticationFilter)
 *
 * WHY @Transactional(readOnly = true)?
 *   - readOnly = true tells Hibernate to skip dirty-checking on entities
 *   - Slightly better performance for pure read operations
 *   - No write operations happen here, so read-only is safe
 *
 * Our User entity already implements UserDetails, so we return it directly.
 * Spring Security then calls getAuthorities(), isEnabled(), isAccountNonLocked()
 * on the returned object to make authorization decisions.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param username — in our system, this is the email address
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username
                ));
    }
}
