package com.rajlaxmi.jewellers.enums;

/**
 * User roles for RBAC (Role-Based Access Control)
 *
 * ADMIN  → full access: product CRUD, order management, user management,
 *          inventory, analytics, gold price override
 * CUSTOMER → browsing, cart, wishlist, order placement, reviews
 *
 * Spring Security reads these roles and enforces them via
 * @PreAuthorize("hasRole('ADMIN')") on controller methods.
 */
public enum Role {
    ADMIN,
    CUSTOMER
}
