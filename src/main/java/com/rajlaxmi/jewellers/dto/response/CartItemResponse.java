package com.rajlaxmi.jewellers.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Single item in the cart — includes live-calculated price */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartItemResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productSlug;
    private String primaryImageUrl;
    private String sku;
    private String metalType;
    private String goldPurity;
    private BigDecimal weightGrams;
    private int quantity;
    private BigDecimal unitPrice;       // live calculated price per unit
    private BigDecimal totalPrice;      // unitPrice × quantity
    private BigDecimal goldRateUsed;    // gold rate at the time of this response
    private boolean isInStock;
    private int availableStock;
    private LocalDateTime addedAt;
}
