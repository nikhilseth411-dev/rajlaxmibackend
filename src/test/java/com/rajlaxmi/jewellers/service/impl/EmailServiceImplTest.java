package com.rajlaxmi.jewellers.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailServiceImplTest {

    @Test
    void sendsOtpThroughResendHttpsApiWhenApiKeyIsConfigured() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailServiceImpl emailService = new EmailServiceImpl(mailSender, WebClient.builder());
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "resendApiKey", "test-resend-key");
        ReflectionTestUtils.setField(emailService, "resendApiUrl",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/emails");
        ReflectionTestUtils.setField(emailService, "fromEmail", "Raj Laxmi <noreply@example.com>");
        ReflectionTestUtils.setField(emailService, "businessName", "Raj Laxmi Jewellers");

        try {
            emailService.sendOtpEmail("customer@example.com", "Customer", "654321");
        } finally {
            server.stop(0);
        }

        assertThat(authorization.get()).isEqualTo("Bearer test-resend-key");
        assertThat(requestBody.get())
                .contains("customer@example.com")
                .contains("noreply@example.com")
                .contains("654321");
        verifyNoInteractions(mailSender);
    }
}
