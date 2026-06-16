package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 * Category Entity — maps to 'categories' table
 * ================================================================
 * Supports a self-referencing parent-child hierarchy:
 *   Parent: "Gold Jewellery"
 *     Child: "Necklaces", "Bangles", "Rings", etc.
 *
 * WHY self-referencing?
 *   Allows unlimited nesting (though we only use 2 levels for now)
 *   without needing separate parent/child tables.
 *   parent = null means it's a root/top-level category.
 *
 * slug:
 *   URL-friendly version of the name. e.g. "Gold Jewellery" → "gold-jewellery"
 *   Used in SEO-friendly URLs: /collections/gold-jewellery
 *   Generated in CategoryService before saving.
 * ================================================================
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * SEO slug: "gold-jewellery", "bridal-collection"
     * Unique — used in frontend route params and meta tags
     */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 300)
    private String description;

    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /**
     * Display order for homepage category grid (lower = shown first)
     */
    @Builder.Default
    private int sortOrder = 0;

    // ── Self-referencing hierarchy ────────────────────────────

    /**
     * Parent category. null = top-level category.
     * LAZY loading: parent is not fetched unless accessed,
     * avoiding unnecessary DB queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Child categories. Not fetched by default (LAZY).
     * Access only when needed (e.g. category tree API).
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

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
