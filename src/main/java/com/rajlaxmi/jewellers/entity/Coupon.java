package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon Entity — maps to 'coupons' table
 *
 * Supports two discount types:
 *   FLAT: deduct fixed ₹ amount (e.g., ₹500 off)
 *   PERCENT: deduct percentage (e.g., 10% off, max cap applies)
 *
 * usageLimit: total times this coupon can be used across all customers.
 * usedCount: auto-incremented on each successful redemption.
 * isActive: admin can disable coupons without deleting.
 */
@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true),
        @Index(name = "idx_coupon_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code; // e.g. "DIWALI500", "FIRSTBUY10"

    @Column(length = 200)
    private String description;

    @Column(nullable = false, length = 10)
    private String discountType; // FLAT | PERCENT

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    // For PERCENT: maximum discount cap in ₹
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    // Minimum order value to apply this coupon
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Builder.Default
    private int usageLimit = 100;

    @Builder.Default
    private int usedCount = 0;

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Helper ─────────────────────────────────────────────────
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive
                && usedCount < usageLimit
                && (validFrom == null || !now.isBefore(validFrom))
                && (validUntil == null || !now.isAfter(validUntil));
    }

    /**
     * Calculates discount amount for a given order total.
     * PERCENT type: min(orderTotal × rate, maxDiscountAmount)
     * FLAT type: min(discountValue, orderTotal)
     */
    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!isValid() || orderTotal.compareTo(minimumOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        if ("PERCENT".equals(discountType)) {
            BigDecimal discount = orderTotal.multiply(discountValue)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            if (maxDiscountAmount != null) {
                discount = discount.min(maxDiscountAmount);
            }
            return discount;
        }
        return discountValue.min(orderTotal);
    }
}
