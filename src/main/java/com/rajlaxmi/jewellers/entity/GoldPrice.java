package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 * GoldPrice Entity — maps to 'gold_prices' table
 * ================================================================
 * Stores BOTH current and historical gold prices.
 *
 * WHY store historical prices?
 *   1. Price charts on homepage (7-day / 30-day trend visualization)
 *   2. If API is down, serve last known price from DB
 *   3. Order records reference gold price at time of purchase
 *      (stored in order_items.gold_rate_at_order — not here)
 *   4. Admin can audit price history
 *
 * HOW prices are fetched:
 *   GoldPriceScheduler runs every hour via @Scheduled.
 *   It calls metals.live API → parses response → saves new record.
 *   Latest record = current live price.
 *
 * PRICE FIELDS:
 *   - rate24K: price per gram for 24K (pure gold) — from API
 *   - rate22K: rate24K × 0.916 (calculated)
 *   - rate18K: rate24K × 0.750 (calculated)
 *   These are always consistent — if admin overrides, all three
 *   are recalculated from the overridden 24K rate.
 *
 * isAdminOverride:
 *   True if admin manually set this rate (bypasses API).
 *   Admin override stays active until next scheduled fetch,
 *   OR until admin removes the override.
 * ================================================================
 */
@Entity
@Table(name = "gold_prices", indexes = {
        @Index(name = "idx_gold_price_fetched_at", columnList = "fetched_at"),
        @Index(name = "idx_gold_price_current", columnList = "is_current")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Price Per Gram (INR) ──────────────────────────────────
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate24K;  // base rate from API

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate22K;  // = rate24K × 0.916

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate18K;  // = rate24K × 0.750

    // ── Price Change (for trending indicator on homepage) ─────
    /**
     * Difference from previous record.
     * Positive = price went up (show green ↑)
     * Negative = price went down (show red ↓)
     */
    @Column(precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal changePercent = BigDecimal.ZERO;

    // ── Metadata ──────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    @Column(length = 20)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 50)
    @Builder.Default
    private String source = "metals.live"; // API source name

    /**
     * Only one record should have isCurrent = true.
     * When new price is fetched, old current record is set to false,
     * new record is saved with isCurrent = true.
     * This allows efficient "get current price" query.
     */
    @Builder.Default
    private boolean isCurrent = false;

    @Builder.Default
    private boolean isAdminOverride = false;

    @PrePersist
    protected void onCreate() {
        if (fetchedAt == null) {
            fetchedAt = LocalDateTime.now();
        }
    }
}
