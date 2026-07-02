package com.rajlaxmi.jewellers.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ProductResponse — used in product listing/search pages.
 * Shows summary info + primary image + live calculated price.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    private Long id;
    private String name;
    private String sku;
    private String slug;
    private String metalType;
    private GoldPurity goldPurity;
    private BigDecimal weightGrams;
    private ProductCategory productCategory;
    private String categoryName;
    private Long categoryId;

    // Primary image only (for listing cards)
    private String primaryImageUrl;

    // Live calculated price (computed by PricingEngine at request time)
    private BigDecimal finalPrice;
    private BigDecimal baseMetalValue;
    private BigDecimal makingChargesValue;
    private BigDecimal makingCharges;
    private String makingChargesType;
    private BigDecimal gstPercentage;
    private BigDecimal gstAmount;
    private BigDecimal currentGoldRate;

    private boolean isBisHallmarked;
    private boolean isFeatured;
    private boolean isNewArrival;
    private boolean isBestSeller;
    private boolean isInStock;
    private int stockQuantity;

    private LocalDateTime createdAt;
}
