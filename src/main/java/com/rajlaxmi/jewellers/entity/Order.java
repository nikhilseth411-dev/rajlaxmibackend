package com.rajlaxmi.jewellers.entity;

import com.rajlaxmi.jewellers.enums.OrderStatus;
import com.rajlaxmi.jewellers.enums.PaymentMethod;
import com.rajlaxmi.jewellers.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity — maps to 'orders' table
 *
 * CRITICAL DESIGN DECISION — Gold Rate Snapshot:
 *   goldRateAtOrder stores the 22K rate at the moment of purchase.
 *   This is essential because:
 *   1. Customer paid based on a specific rate
 *   2. Rate changes hourly — we can never recalculate historical prices
 *   3. Required for invoice generation and dispute resolution
 *   4. GST filing requires declared price, not current price
 *
 * ORDER NUMBER:
 *   Human-readable format: RLJ-2025-000001
 *   Generated in OrderService using sequence.
 *   Used in WhatsApp confirmations, customer communication.
 *
 * PAYMENT DESIGN:
 *   paymentMethod + paymentStatus are denormalized here for
 *   fast order queries. Full payment details are in a separate
 *   Payment entity (linked via order_id).
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_order_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String orderNumber; // RLJ-2025-000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Order Items ───────────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // ── Shipping Address (snapshot at order time) ─────────────
    // Stored as fields, not FK, so address changes don't affect past orders
    @Column(nullable = false)
    private String shippingFullName;
    @Column(nullable = false)
    private String shippingPhone;
    @Column(nullable = false)
    private String shippingStreet;
    private String shippingLandmark;
    @Column(nullable = false)
    private String shippingCity;
    @Column(nullable = false)
    private String shippingState;
    @Column(nullable = false, length = 6)
    private String shippingPincode;

    // ── Pricing Totals ────────────────────────────────────────
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalGst = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal shippingCharge = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    // ── Gold Rate Snapshot ────────────────────────────────────
    @Column(precision = 10, scale = 2)
    private BigDecimal goldRate22KAtOrder; // ₹/gram at time of purchase

    @Column(precision = 10, scale = 2)
    private BigDecimal goldRate24KAtOrder;

    // ── Coupon ────────────────────────────────────────────────
    @Column(length = 20)
    private String couponCode;

    // ── Status ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // ── Payment ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(length = 100)
    private String paymentTransactionId;

    // ── Tracking ──────────────────────────────────────────────
    @Column(length = 100)
    private String trackingNumber;

    @Column(length = 100)
    private String shippingPartner; // "DTDC", "India Post", "BlueDart"

    // ── Notes ─────────────────────────────────────────────────
    @Column(length = 500)
    private String customerNote;

    @Column(length = 500)
    private String adminNote;

    // ── Audit ─────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
