package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "phone_otp_challenges", indexes = {
        @Index(name = "idx_phone_otp_phone", columnList = "phone"),
        @Index(name = "idx_phone_otp_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneOtpChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column(name = "provider_request_id", nullable = false, length = 200)
    private String providerRequestId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private int attempts = 0;

    @Builder.Default
    private boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
