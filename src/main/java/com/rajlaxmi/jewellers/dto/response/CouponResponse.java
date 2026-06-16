package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minimumOrderAmount;
    private int usageLimit;
    private int usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean isActive;
    private boolean isValid;
    // For cart/checkout — how much will this save?
    private BigDecimal applicableDiscountAmount;
}
