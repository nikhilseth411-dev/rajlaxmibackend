package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Fetch all cart items for a user with product + images in one query (avoids N+1)
    @Query("SELECT c FROM Cart c JOIN FETCH c.product p LEFT JOIN FETCH p.images WHERE c.user.id = :userId")
    List<Cart> findByUserIdWithProducts(@Param("userId") Long userId);

    Optional<Cart> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.id = :userId")
    void clearCartByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
