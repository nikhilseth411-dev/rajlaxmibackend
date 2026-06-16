package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * GoldPriceOverrideRequest — POST /admin/gold-rates/override
 * Admin manually sets the gold price, bypassing the API fetch.
 * The override stays active until the next scheduler run
 * OR until admin removes it via DELETE /admin/gold-rates/override
 */
@Data
public class GoldPriceOverrideRequest {
    @NotNull
    @DecimalMin(value = "1000", message = "Gold rate seems too low — must be > ₹1000/gram")
    private BigDecimal rate24KPerGram;

    private String reason; // e.g. "Festival season adjustment"
}
