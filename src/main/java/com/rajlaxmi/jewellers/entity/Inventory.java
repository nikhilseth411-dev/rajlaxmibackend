package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 * Inventory Entity — maps to 'inventory' table
 * ================================================================
 * WHY separate from Product?
 *   Separating inventory from the product keeps concerns clean:
 *   - Product = what the item IS (weight, purity, price components)
 *   - Inventory = how many are AVAILABLE (stock level, alerts)
 *
 *   This also makes it easy to update stock without touching
 *   product data, and supports future multi-location inventory.
 *
 * LOW STOCK ALERT:
 *   When quantity <= lowStockThreshold, admin gets a dashboard alert.
 *   Default threshold = 2 (jewellery is typically low-quantity).
 *
 * isInStock:
 *   Computed field cached here. True if quantity > 0.
 *   Updated whenever quantity changes.
 *   Exposed in product listing API so frontend can show "Out of Stock".
 *
 * reservedQuantity:
 *   Items currently in customer carts/pending orders.
 *   Available stock = quantity - reservedQuantity.
 *   Prevents overselling during checkout.
 * ================================================================
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * OneToOne with Product. Each product has exactly one inventory record.
     * @MapsId uses the same primary key as Product (shared key strategy).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 0;

    /**
     * Reserved = items in carts or pending orders.
     * Actual available = quantity - reservedQuantity.
     */
    @Builder.Default
    private int reservedQuantity = 0;

    /**
     * Alert threshold. When quantity <= this, show low-stock warning in admin.
     * Default 2 — appropriate for premium jewellery (not mass-market).
     */
    @Builder.Default
    private int lowStockThreshold = 2;

    @Builder.Default
    private boolean isInStock = false;

    private LocalDateTime lastUpdated;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
        this.isInStock = this.quantity > 0;
    }

    // ── Helper Methods ────────────────────────────────────────
    public int getAvailableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }

    public boolean isLowStock() {
        return quantity > 0 && quantity <= lowStockThreshold;
    }
}
