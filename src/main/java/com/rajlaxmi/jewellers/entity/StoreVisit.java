package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * StoreVisit Entity — maps to 'store_visits' table
 *
 * Implements the "Book Store Visit" customer feature.
 * Customers can schedule an in-person visit to the shop
 * at Wazirganj, Gaya, Bihar for product viewing or custom orders.
 *
 * Admin confirms or reschedules via Admin Dashboard.
 * WhatsApp confirmation sent on status change.
 *
 * purposeNote: customer describes what they want to see/discuss
 * (e.g., "Bridal set for December wedding, budget ~₹2 lakh")
 */
@Entity
@Table(name = "store_visits", indexes = {
        @Index(name = "idx_visit_user", columnList = "user_id"),
        @Index(name = "idx_visit_date", columnList = "visit_date"),
        @Index(name = "idx_visit_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false, length = 20)
    private String timeSlot; // "10:00-11:00", "11:00-12:00", etc.

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, CONFIRMED, RESCHEDULED, CANCELLED, COMPLETED

    @Column(length = 500)
    private String purposeNote; // What customer wants to see

    @Column(length = 15)
    private String contactPhone;

    @Column(length = 500)
    private String adminNote; // Internal notes by staff

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
