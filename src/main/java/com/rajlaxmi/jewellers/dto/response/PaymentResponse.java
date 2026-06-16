package com.rajlaxmi.jewellers.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajlaxmi.jewellers.enums.PaymentMethod;
import com.rajlaxmi.jewellers.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;

    // UPI details
    private String upiId;
    private String upiQrImageUrl;
    private String utrNumber;

    // Verification
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private String adminNotes;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // UPI deep link for mobile apps
    private String upiDeepLink;
}
