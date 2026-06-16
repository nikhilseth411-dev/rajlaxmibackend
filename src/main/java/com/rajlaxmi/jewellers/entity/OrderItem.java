package com.rajlaxmi.jewellers.entity;

import com.rajlaxmi.jewellers.enums.GoldPurity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * OrderItem Entity — maps to 'order_items' table
 *
 * Stores a complete price snapshot for each product in the order.
 * WHY store all these fields instead of just product_id?
 *   Product details can change after the order is placed:
 *   - Admin updates making charges
 *   - Gold rate changes
 *   - Product is deleted
 *   The order must always reflect what the customer PAID for,
 *   not the current state of the product.
 *
 * This is a legal requirement for GST invoicing.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Soft reference — product may be deleted, order history must survive
    @Column(name = "product_id")
    private Long productId;

    // ── Price Snapshot (never changes after order is placed) ──
    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, length = 50)
    private String productSku;

    @Column(length = 100)
    private String productSlug;

    private String primaryImageUrl;

    @Column(length = 20)
    private String metalType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GoldPurity goldPurity;

    @Column(precision = 8, scale = 3)
    private BigDecimal weightGrams;

    // Price components at time of purchase
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal goldRateUsed;     // ₹/gram for this purity at purchase

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseMetalValue;   // weight × rate

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal makingCharges;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal stoneCharges = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gstAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;        // final price per unit

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;       // unitPrice × quantity

    private boolean isBisHallmarked;
    private String bisHallmarkNumber;
}
