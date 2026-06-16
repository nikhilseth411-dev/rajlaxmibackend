package com.rajlaxmi.jewellers.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ProductDetailResponse — used on the product detail page.
 * Includes ALL images, full price breakdown, specifications.
 * Transparency in pricing (showing each component) builds trust
 * for Indian jewellery buyers — a core business requirement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String sku;
    private String slug;
    private String description;

    // Category info
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private ProductCategory productCategory;

    // Metal & Purity
    private String metalType;
    private GoldPurity goldPurity;
    private String goldPurityDisplay;  // "22K (91.6% Pure Gold)"
    private String bisHallmarkCode;    // "916"

    // Weight
    private BigDecimal weightGrams;

    // ── Complete Price Breakdown (shown on product page) ──────
    // This transparency is a major trust signal for jewellery buyers
    private BigDecimal currentGoldRatePerGram;   // live rate used
    private BigDecimal baseMetalValue;            // weight × rate × purity
    private BigDecimal makingCharges;             // labour/craftsmanship cost
    private String makingChargesType;             // PER_GRAM or FIXED
    private BigDecimal stoneCharges;              // diamond/stone charges
    private BigDecimal taxableValue;              // base + making + stone
    private BigDecimal gstPercentage;             // 3% (standard)
    private BigDecimal gstAmount;                 // actual GST rupees
    private BigDecimal finalPrice;                // total price to pay

    // Certification
    private boolean isBisHallmarked;
    private String bisHallmarkNumber;

    // Specifications
    private String occasion;
    private String gender;
    private String dimensions;
    private String finish;

    // Images (all, sorted by sortOrder)
    private List<ProductImageResponse> images;

    // Inventory
    private InventoryResponse inventory;

    // Flags
    private boolean isFeatured;
    private boolean isNewArrival;
    private boolean isBestSeller;
    private boolean isActive;

    // SEO
    private String metaTitle;
    private String metaDescription;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related products (same category)
    private List<ProductResponse> relatedProducts;
}
