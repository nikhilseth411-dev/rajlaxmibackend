package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.GoldPrice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * GoldPriceRepository
 *
 * Key operations:
 *   - findCurrentPrice(): the one record with isCurrent = true
 *   - findPriceHistory(): last N records for chart data
 *   - markAllAsNotCurrent(): called before inserting new current price
 */
@Repository
public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {

    Optional<GoldPrice> findByIsCurrentTrue();

    // Get last N records for price history chart (ordered by fetchedAt DESC)
    List<GoldPrice> findAllByOrderByFetchedAtDesc(Pageable pageable);

    // Mark all records as not current — called before saving new current price
    @Modifying
    @Query("UPDATE GoldPrice g SET g.isCurrent = false WHERE g.isCurrent = true")
    void markAllAsNotCurrent();
}
