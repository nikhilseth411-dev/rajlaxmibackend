package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.UpdateAdminCredentialsRequest;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.DuplicateResourceException;
import com.rajlaxmi.jewellers.repository.RefreshTokenRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminAccountService adminAccountService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(7L)
                .firstName("Shop")
                .lastName("Owner")
                .email("old-admin@example.com")
                .password("existing-bcrypt-hash")
                .role(Role.ADMIN)
                .isEmailVerified(true)
                .isActive(true)
                .build();
    }

    @Test
    void updatesEmailAndBcryptPasswordThenRevokesSessions() {
        UpdateAdminCredentialsRequest request = request("  Owner@Example.com  ", "NewPass@123", "NewPass@123");
        when(passwordEncoder.matches("CurrentPass@123", "existing-bcrypt-hash")).thenReturn(true);
        when(userRepository.existsByEmailAndIdNot("owner@example.com", 7L)).thenReturn(false);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-bcrypt-hash");

        var response = adminAccountService.updateCredentials(admin, request);

        assertThat(admin.getEmail()).isEqualTo("owner@example.com");
        assertThat(admin.getPassword()).isEqualTo("new-bcrypt-hash");
        assertThat(response.getData().getEmail()).isEqualTo("owner@example.com");
        verify(userRepository).save(admin);
        verify(refreshTokenRepository).revokeAllByUserId(7L);
    }

    @Test
    void bcryptHashAcceptsOnlyTheNewPasswordAfterRotation() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        RefreshTokenRepository tokens = org.mockito.Mockito.mock(RefreshTokenRepository.class);
        AdminAccountService service = new AdminAccountService(users, tokens, bcrypt);
        admin.setPassword(bcrypt.encode("CurrentPass@123"));
        when(users.existsByEmailAndIdNot("owner@example.com", 7L)).thenReturn(false);

        service.updateCredentials(
                admin,
                request("owner@example.com", "NewPass@123", "NewPass@123"));

        assertThat(bcrypt.matches("NewPass@123", admin.getPassword())).isTrue();
        assertThat(bcrypt.matches("CurrentPass@123", admin.getPassword())).isFalse();
        verify(tokens).revokeAllByUserId(7L);
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        UpdateAdminCredentialsRequest request = request("owner@example.com", "NewPass@123", "NewPass@123");
        when(passwordEncoder.matches("CurrentPass@123", "existing-bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> adminAccountService.updateCredentials(admin, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Current password is incorrect.");

        verify(userRepository, never()).save(admin);
        verify(refreshTokenRepository, never()).revokeAllByUserId(7L);
    }

    @Test
    void rejectsEmailUsedByAnotherAccount() {
        UpdateAdminCredentialsRequest request = request("owner@example.com", "NewPass@123", "NewPass@123");
        when(passwordEncoder.matches("CurrentPass@123", "existing-bcrypt-hash")).thenReturn(true);
        when(userRepository.existsByEmailAndIdNot("owner@example.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> adminAccountService.updateCredentials(admin, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(admin);
        verify(passwordEncoder, never()).encode("NewPass@123");
    }

    @Test
    void rejectsMismatchedNewPasswords() {
        UpdateAdminCredentialsRequest request = request("owner@example.com", "NewPass@123", "Different@123");
        when(passwordEncoder.matches("CurrentPass@123", "existing-bcrypt-hash")).thenReturn(true);

        assertThatThrownBy(() -> adminAccountService.updateCredentials(admin, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("New passwords do not match.");

        verify(userRepository, never()).save(admin);
    }

    private UpdateAdminCredentialsRequest request(
            String newEmail,
            String newPassword,
            String confirmPassword) {
        UpdateAdminCredentialsRequest request = new UpdateAdminCredentialsRequest();
        request.setNewEmail(newEmail);
        request.setCurrentPassword("CurrentPass@123");
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
