package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SilverPrice Entity — maps to 'silver_prices' table
 *
 * Same structure as GoldPrice but for silver.
 * Silver purity tiers:
 *   - 925 Sterling Silver (92.5% pure) — most common
 *   - 999 Fine Silver (99.9% pure) — coins/bullion
 *
 * For jewellery pricing, we store price per gram for 925 silver.
 * fetched from same metals.live API call as gold (bundled request).
 */
@Entity
@Table(name = "silver_prices", indexes = {
        @Index(name = "idx_silver_price_current", columnList = "is_current"),
        @Index(name = "idx_silver_price_fetched_at", columnList = "fetched_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SilverPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal ratePerGram;   // 925 sterling silver price per gram

    @Column(precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal changePercent = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private String source = "gold-api.com";

    @Builder.Default
    private boolean isCurrent = false;

    @Builder.Default
    private boolean isAdminOverride = false;

    @PrePersist
    protected void onCreate() {
        if (fetchedAt == null) fetchedAt = LocalDateTime.now();
    }
}
