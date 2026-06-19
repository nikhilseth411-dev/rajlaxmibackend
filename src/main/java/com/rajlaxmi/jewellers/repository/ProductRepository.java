package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.enums.GoldPurity;
import com.rajlaxmi.jewellers.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ================================================================
 * ProductRepository
 * ================================================================
 * Key query design decisions:
 *
 * 1. All customer-facing queries filter by isActive = true.
 *    Admin queries can see all products including inactive ones.
 *
 * 2. Search uses LOWER() for case-insensitive matching.
 *    For production with large catalog, consider full-text search
 *    or Elasticsearch. For V1, LIKE queries are sufficient.
 *
 * 3. JOIN FETCH images in detail query to avoid N+1 problem.
 *    Without it, each product.getImages() call = separate DB query.
 *    With JOIN FETCH, all images are loaded in a single query.
 *
 * 4. @Query uses JPQL — portable across PostgreSQL and any JPA DB.
 * ================================================================
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── Customer Browsing ─────────────────────────────────────

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    long countByCategoryIdAndIsActiveTrue(Long categoryId);

    Page<Product> findByProductCategoryAndIsActiveTrue(ProductCategory category, Pageable pageable);

    Page<Product> findByGoldPurityAndIsActiveTrue(GoldPurity purity, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND (:keyword IS NULL OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:productCategory IS NULL OR p.productCategory = :productCategory)
              AND (:goldPurity IS NULL OR p.goldPurity = :goldPurity)
              AND (:metalType IS NULL OR LOWER(p.metalType) = :metalType)
              AND (:occasion IS NULL OR LOWER(p.occasion) = :occasion)
              AND (:gender IS NULL OR LOWER(p.gender) = :gender)
              AND (:isFeatured IS NULL OR p.isFeatured = :isFeatured)
              AND (:isNewArrival IS NULL OR p.isNewArrival = :isNewArrival)
              AND (:isBestSeller IS NULL OR p.isBestSeller = :isBestSeller)
            """)
    Page<Product> findActiveByFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("productCategory") ProductCategory productCategory,
            @Param("goldPurity") GoldPurity goldPurity,
            @Param("metalType") String metalType,
            @Param("occasion") String occasion,
            @Param("gender") String gender,
            @Param("isFeatured") Boolean isFeatured,
            @Param("isNewArrival") Boolean isNewArrival,
            @Param("isBestSeller") Boolean isBestSeller,
            Pageable pageable);

    // ── Product Detail (with images — avoids N+1) ─────────────
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id AND p.isActive = true")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.slug = :slug AND p.isActive = true")
    Optional<Product> findBySlugWithImages(@Param("slug") String slug);

    // ── Homepage Sections ─────────────────────────────────────
    List<Product> findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findByIsNewArrivalTrueAndIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findByIsBestSellerTrueAndIsActiveTrueOrderByCreatedAtDesc();

    // ── Search ────────────────────────────────────────────────
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    // ── Related Products (same category, exclude self) ────────
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId " +
           "AND p.id != :excludeId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId,
                                      @Param("excludeId") Long excludeId,
                                      Pageable pageable);

    // ── Admin ─────────────────────────────────────────────────
    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    long countByIsActiveTrue();

    // ── Low Stock Alert (for admin dashboard) ─────────────────
    @Query("SELECT p FROM Product p JOIN p.inventory i " +
           "WHERE i.quantity <= i.lowStockThreshold AND i.quantity > 0 AND p.isActive = true")
    List<Product> findLowStockProducts();

    // ── Out of Stock ──────────────────────────────────────────
    @Query("SELECT p FROM Product p JOIN p.inventory i WHERE i.quantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProducts();
}
