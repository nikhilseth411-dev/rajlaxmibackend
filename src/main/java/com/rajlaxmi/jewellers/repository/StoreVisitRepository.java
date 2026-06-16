package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.StoreVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StoreVisitRepository extends JpaRepository<StoreVisit, Long> {

    Page<StoreVisit> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<StoreVisit> findByStatusOrderByVisitDateAsc(String status, Pageable pageable);

    List<StoreVisit> findByVisitDateAndStatus(LocalDate date, String status);

    long countByStatus(String status);
}
