package com.rajlaxmi.jewellers.entity;

import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 * Product Entity — maps to 'products' table
 * ================================================================
 * This is the central entity of the entire platform.
 *
 * PRICING DESIGN:
 *   We do NOT store finalPrice in the DB because it changes every
 *   hour when gold rates update. Instead, we store:
 *     - weightGrams: physical weight of the jewellery piece
 *     - goldPurity: 18K / 22K / 24K
 *     - makingCharges: admin-set, flat ₹ per gram OR fixed ₹
 *     - stoneCharges: fixed ₹ for stones/diamonds (optional)
 *     - gstPercentage: default 3%, admin-configurable
 *
 *   Final price is CALCULATED on-the-fly in PricingEngine.java:
 *     Base = weightGrams × liveGoldRate × (purity% / 100)
 *     Total = Base + makingCharges + stoneCharges + GST
 *
 *   This ensures prices are always accurate and updated every hour.
 *
 * METAL TYPE:
 *   metalType = "GOLD" | "SILVER" | "DIAMOND"
 *   For SILVER products, silverRate from SilverPrice is used.
 *   For DIAMOND, diamondCharges are the primary cost component.
 *
 * BIS HALLMARK:
 *   isBisHallmarked + bisHallmarkNumber for trust/certification display
 *   This is a major trust signal for Indian jewellery buyers.
 * ================================================================
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_active", columnList = "is_active"),
        @Index(name = "idx_product_purity", columnList = "gold_purity")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Basic Info ────────────────────────────────────────────
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * SKU = Stock Keeping Unit. Unique identifier for inventory.
     * Format: RLJ-GOLD-001, RLJ-BANGLE-042, etc.
     * Used for: inventory tracking, bulk CSV import/export
     */
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Category ──────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Convenience enum field for fast filtering without JOIN.
     * Redundant with category but improves query performance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", length = 30)
    private ProductCategory productCategory;

    // ── Metal & Purity ────────────────────────────────────────
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String metalType = "GOLD"; // GOLD, SILVER, DIAMOND

    @Enumerated(EnumType.STRING)
    @Column(name = "gold_purity", length = 10)
    private GoldPurity goldPurity; // null for silver products

    // ── Weight & Pricing Components ───────────────────────────
    /**
     * Physical weight of the jewellery in grams.
     * CRITICAL for price calculation.
     * Stored as BigDecimal for precision (e.g. 4.35 grams)
     */
    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal weightGrams;

    /**
     * Making charges set by admin.
     * Flat ₹ amount per gram OR total fixed ₹ for the piece.
     * Controlled by makingChargesType field below.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal makingCharges = BigDecimal.ZERO;

    /**
     * "PER_GRAM" = makingCharges is multiplied by weightGrams
     * "FIXED"    = makingCharges is a flat amount for the whole piece
     */
    @Column(length = 10)
    @Builder.Default
    private String makingChargesType = "PER_GRAM";

    /**
     * Additional charges for diamonds or precious stones.
     * Fixed ₹ amount per product. Zero if no stones.
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stoneCharges = BigDecimal.ZERO;

    /**
     * GST percentage. Standard = 3% for gold jewellery.
     * Admin can override per product if structure changes.
     */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercentage = new BigDecimal("3.00");

    // ── BIS Hallmark ──────────────────────────────────────────
    @Builder.Default
    private boolean isBisHallmarked = true;

    @Column(length = 20)
    private String bisHallmarkNumber;  // e.g. "P-1234567"

    // ── Product Specifications ────────────────────────────────
    @Column(length = 50)
    private String occasion;     // Bridal, Daily Wear, Festival, etc.

    @Column(length = 50)
    private String gender;       // Women, Men, Kids, Unisex

    @Column(length = 100)
    private String dimensions;   // "Length: 18 inches" or "Size: 7"

    @Column(length = 100)
    private String finish;       // "High Polish", "Matte", "Antique"

    // ── Inventory & Visibility ────────────────────────────────
    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isFeatured = false;   // shown in Featured section on homepage

    @Builder.Default
    private boolean isNewArrival = false;

    @Builder.Default
    private boolean isBestSeller = false;

    // ── SEO Fields ────────────────────────────────────────────
    @Column(length = 200)
    private String metaTitle;

    @Column(length = 350)
    private String metaDescription;

    @Column(length = 150, unique = true)
    private String slug;  // URL: /products/22k-gold-mangalsutra-001

    // ── Images ───────────────────────────────────────────────
    /**
     * One product has many images (multiple angles, zoom views).
     * CascadeType.ALL: deleting a product also deletes its images.
     * orphanRemoval: if an image is removed from this list, it's deleted from DB.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // ── Inventory ─────────────────────────────────────────────
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL,
              orphanRemoval = true, fetch = FetchType.LAZY)
    private Inventory inventory;

    // ── Audit ─────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
