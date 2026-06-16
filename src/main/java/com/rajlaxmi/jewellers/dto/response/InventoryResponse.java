package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventoryResponse {
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int lowStockThreshold;
    private boolean isInStock;
    private boolean isLowStock;
    private LocalDateTime lastUpdated;
}
