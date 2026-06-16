package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {
    @NotBlank @Size(min = 3, max = 20)
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Coupon code must be uppercase alphanumeric")
    private String code;

    @Size(max = 200) private String description;

    @NotBlank @Pattern(regexp = "^(FLAT|PERCENT)$")
    private String discountType;

    @NotNull @DecimalMin("1")
    private BigDecimal discountValue;

    @DecimalMin("0") private BigDecimal maxDiscountAmount;
    @DecimalMin("0") private BigDecimal minimumOrderAmount;

    @Min(1) private int usageLimit = 100;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
