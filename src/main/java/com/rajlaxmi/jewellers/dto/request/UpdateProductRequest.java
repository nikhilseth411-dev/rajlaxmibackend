package com.rajlaxmi.jewellers.dto.request;

import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/** PATCH /admin/products/{id} — partial update, all fields optional */
@Data
public class UpdateProductRequest {
    @Size(min = 3, max = 200) private String name;
    private String description;
    private Long categoryId;
    private ProductCategory productCategory;
    @Pattern(regexp = "^(GOLD|SILVER|DIAMOND)$", message = "Metal type must be GOLD, SILVER, or DIAMOND")
    private String metalType;
    private GoldPurity goldPurity;
    @DecimalMin("0.01") private BigDecimal weightGrams;
    @DecimalMin("0") private BigDecimal makingCharges;
    private String makingChargesType;
    @DecimalMin("0") private BigDecimal stoneCharges;
    @DecimalMin("0") @DecimalMax("28") private BigDecimal gstPercentage;
    private Boolean isBisHallmarked;
    private String bisHallmarkNumber;
    private String occasion;
    private String gender;
    private String dimensions;
    private String finish;
    private Boolean isFeatured;
    private Boolean isNewArrival;
    private Boolean isBestSeller;
    private Boolean isActive;
    private String metaTitle;
    private String metaDescription;
}
