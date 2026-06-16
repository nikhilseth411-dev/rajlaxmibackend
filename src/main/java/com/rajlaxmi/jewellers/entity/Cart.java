package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 * Cart Entity — maps to 'carts' table
 * ================================================================
 * Each row = one product in one user's cart.
 * Multiple rows per user = multiple cart items.
 *
 * WHY not store cart price here?
 *   Gold prices change hourly. Storing price in cart would show
 *   stale prices. Instead, cart price is calculated fresh on every
 *   cart fetch using current gold rates.
 *
 *   This is intentional and important for jewellery e-commerce:
 *   "Price at checkout may differ from price when added to cart."
 *   The frontend shows a disclaimer for this.
 *
 * Unique constraint on (user_id, product_id):
 *   A user can't add the same product twice — instead the quantity
 *   is updated. Enforced at DB level AND in CartService.
 * ================================================================
 */
@Entity
@Table(name = "carts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_user_product",
                columnNames = {"user_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_cart_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
