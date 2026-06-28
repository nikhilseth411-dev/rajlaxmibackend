package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.LoginRequest;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.repository.RefreshTokenRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.EmailService;
import com.rajlaxmi.jewellers.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;

    @InjectMocks private AuthServiceImpl authService;

    private User user;
    private LoginRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutDurationMin", 30);

        user = User.builder()
                .id(1L)
                .firstName("Customer")
                .lastName("One")
                .email("customer@example.com")
                .password("encoded-password")
                .role(Role.CUSTOMER)
                .isEmailVerified(true)
                .isActive(true)
                .failedLoginAttempts(0)
                .build();

        request = new LoginRequest();
        request.setEmail("  Customer@Example.com  ");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));
    }

    @Test
    void badPasswordConsumesExactlyOneAttempt() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals(1, user.getFailedLoginAttempts());
        assertEquals("Invalid email or password. 4 attempt(s) remaining before account lock.", error.getMessage());
        verify(userRepository).save(user);
    }

    @Test
    void infrastructureFailureDoesNotConsumeLoginAttempt() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new InternalAuthenticationServiceException("database unavailable"));

        assertThrows(
                InternalAuthenticationServiceException.class,
                () -> authService.login(request)
        );

        assertEquals(0, user.getFailedLoginAttempts());
        verify(userRepository, never()).save(any(User.class));
    }
}
