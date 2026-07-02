package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.PhoneOtpRequest;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.repository.PhoneOtpChallengeRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.AuthService;
import com.rajlaxmi.jewellers.service.SmsOtpProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PhoneAuthServiceImplTest {
    @Test
    void doesNotGenerateFallbackOtpWhenSmsIsDisabled() {
        SmsOtpProvider provider = mock(SmsOtpProvider.class);
        PhoneAuthServiceImpl service = new PhoneAuthServiceImpl(
                provider,
                mock(PhoneOtpChallengeRepository.class),
                mock(UserRepository.class),
                mock(PasswordEncoder.class),
                mock(AuthService.class)
        );
        ReflectionTestUtils.setField(service, "smsEnabled", false);
        ReflectionTestUtils.setField(service, "smsProvider", "MSG91");
        PhoneOtpRequest request = new PhoneOtpRequest();
        request.setPhone("9876543210");

        assertThatThrownBy(() -> service.requestOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("SMS OTP service is not configured yet.");
        verifyNoInteractions(provider);
    }
}
