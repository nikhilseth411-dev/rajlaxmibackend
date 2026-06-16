package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * CartResponse — full cart with all items and totals.
 * Prices are ALWAYS calculated fresh using live gold rates.
 * priceDisclaimer is shown on the UI as a trust note.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;
    private BigDecimal totalGst;
    private BigDecimal grandTotal;
    private BigDecimal currentGoldRate22K;   // shown so customer knows rate used
    private String priceDisclaimer;          // "Prices are live and may change"
}
