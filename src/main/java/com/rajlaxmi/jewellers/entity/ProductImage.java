package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * ================================================================
 * ProductImage Entity — maps to 'product_images' table
 * ================================================================
 * Each product can have multiple images:
 *   - Primary image (shown in listings)
 *   - Additional images (shown in product detail gallery)
 *   - Different angles, zoom views
 *
 * sortOrder: determines display sequence in the gallery.
 * Lower sortOrder = shown first. Primary image = sortOrder 0.
 *
 * imageUrl: relative path or full URL.
 *   For local storage: "uploads/products/sku-001-front.jpg"
 *   For CDN: "https://cdn.rajlaxmi.com/products/sku-001-front.webp"
 *
 * altText: important for SEO and accessibility.
 *   e.g. "22K gold mangalsutra with diamond pendant - front view"
 * ================================================================
 */
@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_images_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String imageUrl;

    @Column(length = 200)
    private String altText;

    @Builder.Default
    private boolean isPrimary = false;

    @Builder.Default
    private int sortOrder = 0;
}
