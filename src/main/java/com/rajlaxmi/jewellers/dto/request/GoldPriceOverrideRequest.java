package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * GoldPriceOverrideRequest — POST /admin/gold-rates/override
 * Admin manually sets the gold price, bypassing the API fetch.
 * The override remains active until an admin saves another manual rate.
 */
@Data
public class GoldPriceOverrideRequest {
    @NotNull
    @DecimalMin(value = "1000", message = "Gold rate seems too low — must be > ₹1000/gram")
    private BigDecimal rate24KPerGram;

    @DecimalMin(value = "1000", message = "22K gold rate must be at least 1000/gram")
    private BigDecimal rate22KPerGram;

    @DecimalMin(value = "1000", message = "18K gold rate must be at least 1000/gram")
    private BigDecimal rate18KPerGram;

    private String reason; // e.g. "Festival season adjustment"
}
