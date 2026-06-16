package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.Order;
import com.rajlaxmi.jewellers.enums.OrderStatus;
import com.rajlaxmi.jewellers.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.paymentStatus = :status")
    BigDecimal sumRevenueByPaymentStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.createdAt >= :from AND o.paymentStatus = 'SUCCESS'")
    BigDecimal sumRevenueSince(@Param("from") LocalDateTime from);

    // Generate next order number using count
    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();
}
