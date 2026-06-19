package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.quantity = i.quantity - :qty,
                i.isInStock = CASE WHEN (i.quantity - :qty) > 0 THEN true ELSE false END,
                i.lastUpdated = CURRENT_TIMESTAMP
            WHERE i.product.id = :productId
              AND (i.quantity - i.reservedQuantity) >= :qty
            """)
    int decrementStock(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.quantity = i.quantity + :qty,
                i.isInStock = CASE WHEN (i.quantity + :qty) > 0 THEN true ELSE false END,
                i.lastUpdated = CURRENT_TIMESTAMP
            WHERE i.product.id = :productId
            """)
    void incrementStock(@Param("productId") Long productId, @Param("qty") int qty);
}
