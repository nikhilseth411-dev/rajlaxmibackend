package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateInventoryRequest {
    @NotNull private Long productId;
    @Min(0) private int quantity;
    @Min(1) private int lowStockThreshold = 2;
    private String reason; // "Restock", "Damaged", "Return", "Manual correction"
}
