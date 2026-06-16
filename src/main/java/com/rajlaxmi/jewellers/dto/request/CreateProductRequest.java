package com.rajlaxmi.jewellers.dto.request;

import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CreateProductRequest — POST /admin/products
 *
 * Admin creates a new jewellery product via this DTO.
 * The pricing fields (weightGrams, goldPurity, makingCharges, gstPercentage)
 * feed directly into the PricingEngine to compute the final selling price.
 *
 * Validation ensures no product is created with invalid data
 * that could cause pricing calculation errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200)
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^RLJ-[A-Z]+-\\d{3,6}$",
             message = "SKU format: RLJ-CATEGORY-001 (e.g. RLJ-GOLD-001)")
    private String sku;

    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private ProductCategory productCategory;

    @NotBlank(message = "Metal type is required")
    @Pattern(regexp = "^(GOLD|SILVER|DIAMOND)$", message = "Metal type must be GOLD, SILVER, or DIAMOND")
    private String metalType;

    private GoldPurity goldPurity;  // required for GOLD products

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than 0")
    @DecimalMax(value = "9999.99", message = "Weight cannot exceed 9999.99 grams")
    private BigDecimal weightGrams;

    @NotNull
    @DecimalMin(value = "0", message = "Making charges cannot be negative")
    private BigDecimal makingCharges;

    @Pattern(regexp = "^(PER_GRAM|FIXED)$", message = "Making charges type must be PER_GRAM or FIXED")
    private String makingChargesType = "PER_GRAM";

    @DecimalMin(value = "0")
    private BigDecimal stoneCharges = BigDecimal.ZERO;

    @DecimalMin(value = "0") @DecimalMax(value = "28")
    private BigDecimal gstPercentage = new BigDecimal("3.00");

    private boolean isBisHallmarked = true;
    private String bisHallmarkNumber;

    private String occasion;
    private String gender;
    private String dimensions;
    private String finish;

    private boolean isFeatured = false;
    private boolean isNewArrival = false;
    private boolean isBestSeller = false;

    private String metaTitle;
    private String metaDescription;

    // Initial stock quantity (creates Inventory record)
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQuantity = 0;

    @Min(value = 1)
    private int lowStockThreshold = 2;
}
