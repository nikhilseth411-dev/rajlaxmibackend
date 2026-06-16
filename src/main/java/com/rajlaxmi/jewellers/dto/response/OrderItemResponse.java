package com.rajlaxmi.jewellers.dto.response;

import com.rajlaxmi.jewellers.enums.GoldPurity;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productSlug;
    private String primaryImageUrl;
    private String metalType;
    private GoldPurity goldPurity;
    private BigDecimal weightGrams;
    private BigDecimal goldRateUsed;
    private BigDecimal baseMetalValue;
    private BigDecimal makingCharges;
    private BigDecimal stoneCharges;
    private BigDecimal gstPercentage;
    private BigDecimal gstAmount;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal totalPrice;
    private boolean isBisHallmarked;
    private String bisHallmarkNumber;
}
