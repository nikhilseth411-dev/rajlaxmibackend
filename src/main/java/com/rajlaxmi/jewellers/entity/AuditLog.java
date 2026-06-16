package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 * AuditLog Entity — maps to 'audit_logs' table
 * ================================================================
 * Records every significant admin action for accountability and debugging.
 *
 * WHY audit logs?
 *   1. Accountability: know who changed what and when
 *   2. Debugging: trace what happened before a bug/issue
 *   3. Compliance: business record keeping
 *   4. Security: detect unauthorized access patterns
 *   5. Interview point: shows understanding of production concerns
 *
 * WHAT is logged:
 *   - Product created/updated/deleted
 *   - Price overrides (gold rate manually changed)
 *   - Order status changes
 *   - User account actions (lock/unlock/role change)
 *   - Inventory updates
 *   - Admin login/logout
 *
 * oldValue / newValue:
 *   Stored as JSON strings for flexibility.
 *   Example: oldValue = {"quantity": 5}, newValue = {"quantity": 3}
 *   Not strictly typed — allows logging any entity change.
 * ================================================================
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;       // who performed the action

    @Column(nullable = false, length = 50)
    private String userEmail;

    @Column(nullable = false, length = 50)
    private String action;     // CREATE, UPDATE, DELETE, LOGIN, OVERRIDE, etc.

    @Column(nullable = false, length = 50)
    private String entityType; // PRODUCT, ORDER, USER, GOLD_PRICE, INVENTORY

    private Long entityId;     // ID of the affected entity

    @Column(columnDefinition = "TEXT")
    private String oldValue;   // JSON string of before state

    @Column(columnDefinition = "TEXT")
    private String newValue;   // JSON string of after state

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    @Column(length = 300)
    private String description; // human-readable summary

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
