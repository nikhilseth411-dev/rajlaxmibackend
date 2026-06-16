package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GoldRateResponse — GET /gold-rates/current
 * Displayed in the "Today's Gold Rate" section on homepage.
 * changeAmount/changePercent are used for green↑ / red↓ indicators.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GoldRateResponse {
    // Current rates per gram (INR)
    private BigDecimal rate24K;
    private BigDecimal rate22K;
    private BigDecimal rate18K;
    private BigDecimal silverRatePerGram;

    // Price movement since last update
    private BigDecimal goldChangeAmount;
    private BigDecimal goldChangePercent;
    private boolean goldPriceUp;      // true = green arrow, false = red arrow

    private LocalDateTime lastUpdated;
    private String source;            // "metals.live"
    private boolean isAdminOverride;

    // For chart: last 7 or 30 data points
    private List<PriceHistoryPoint> history;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PriceHistoryPoint {
        private LocalDateTime timestamp;
        private BigDecimal rate24K;
        private BigDecimal rate22K;
    }
}
