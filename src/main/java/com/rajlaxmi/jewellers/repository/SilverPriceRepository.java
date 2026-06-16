package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.SilverPrice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SilverPriceRepository extends JpaRepository<SilverPrice, Long> {

    Optional<SilverPrice> findByIsCurrentTrue();

    List<SilverPrice> findAllByOrderByFetchedAtDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE SilverPrice s SET s.isCurrent = false WHERE s.isCurrent = true")
    void markAllAsNotCurrent();
}
