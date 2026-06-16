package com.rajlaxmi.jewellers.entity;

import com.rajlaxmi.jewellers.enums.PaymentMethod;
import com.rajlaxmi.jewellers.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Entity — maps to 'payments' table
 *
 * Stores full payment details for every order.
 * The Order entity has denormalized paymentStatus/paymentMethod
 * for fast queries. This table has the full audit trail.
 *
 * UPI PAYMENT (primary method for this business):
 *   upiId = "nitinseth753@okhdfcbank"
 *   QR code is shown to customer at checkout.
 *   Customer pays, enters UTR (Unique Transaction Reference).
 *   Admin verifies and confirms the order.
 *
 * This is a COD/UPI-first business (no Razorpay/Stripe needed yet).
 * The payment flow is: customer pays → shares UTR → admin verifies.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "order_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_utr", columnList = "utr_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // ── UPI Payment Fields ────────────────────────────────────
    /**
     * UPI ID displayed to customer on checkout.
     * Primary business UPI: nitinseth753@okhdfcbank
     */
    @Column(length = 100)
    private String upiId;

    /**
     * UTR = Unique Transaction Reference
     * 12-digit number from UPI app after successful payment.
     * Customer submits this, admin verifies.
     */
    @Column(name = "utr_number", length = 50)
    private String utrNumber;

    /**
     * Base64-encoded QR code image for UPI payment.
     * Generated from UPI deep link at checkout time.
     * Stored for display on order confirmation page.
     */
    @Column(name = "upi_qr_image_url", length = 500)
    private String upiQrImageUrl;

    // ── Gateway Fields (future: Razorpay integration) ─────────
    @Column(length = 100)
    private String gatewayOrderId;

    @Column(length = 100)
    private String gatewayPaymentId;

    @Column(length = 500)
    private String gatewaySignature;

    // ── Cash on Delivery ──────────────────────────────────────
    @Column(length = 200)
    private String codNotes;

    // ── Admin Verification ────────────────────────────────────
    @Column(length = 500)
    private String adminNotes;

    private LocalDateTime verifiedAt;

    @Column(length = 100)
    private String verifiedBy; // admin email

    // ── Failure Handling ──────────────────────────────────────
    @Column(length = 500)
    private String failureReason;

    // ── Audit ─────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
