package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.service.SmsOtpProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Msg91SmsOtpProvider implements SmsOtpProvider {
    private static final Duration TIMEOUT = Duration.ofSeconds(12);
    private final WebClient.Builder webClientBuilder;

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.widget-id:}")
    private String widgetId;

    @Override
    @SuppressWarnings("unchecked")
    public String sendOtp(String phone) {
        validateConfiguration();
        Map<String, Object> response;
        try {
            response = webClientBuilder.baseUrl("https://api.msg91.com")
                    .build()
                    .post()
                    .uri("/api/v5/widget/sendOtp")
                    .header("authkey", apiKey)
                    .bodyValue(Map.of("widgetId", widgetId, "identifier", "91" + phone))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);
        } catch (RuntimeException exception) {
            throw new BusinessException("Unable to send SMS OTP right now. Please try again.");
        }

        if (response == null || !"success".equalsIgnoreCase(String.valueOf(response.get("type")))) {
            throw new BusinessException("Unable to send SMS OTP right now. Please try again.");
        }
        Object requestId = response.get("message");
        if (requestId == null || requestId.toString().isBlank()) {
            throw new BusinessException("SMS provider did not return a verification request.");
        }
        return requestId.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean verifyOtp(String requestId, String otp) {
        validateConfiguration();
        Map<String, Object> response;
        try {
            response = webClientBuilder.baseUrl("https://api.msg91.com")
                    .build()
                    .post()
                    .uri("/api/v5/widget/verifyOtp")
                    .header("authkey", apiKey)
                    .bodyValue(Map.of("widgetId", widgetId, "reqId", requestId, "otp", otp))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);
        } catch (RuntimeException exception) {
            throw new BusinessException("Unable to verify SMS OTP right now. Please try again.");
        }
        return response != null && "success".equalsIgnoreCase(String.valueOf(response.get("type")));
    }

    private void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank() || widgetId == null || widgetId.isBlank()) {
            throw new BusinessException("SMS OTP service is not configured yet.");
        }
    }
}
