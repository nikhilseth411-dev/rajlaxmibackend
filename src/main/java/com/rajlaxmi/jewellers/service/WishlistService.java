package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CartResponse;
import com.rajlaxmi.jewellers.dto.response.WishlistResponse;

public interface WishlistService {
    WishlistResponse getWishlist(Long userId);
    ApiResponse<String> addToWishlist(Long userId, Long productId);
    ApiResponse<String> removeFromWishlist(Long userId, Long productId);
    ApiResponse<CartResponse> moveToCart(Long userId, Long productId);
    boolean isInWishlist(Long userId, Long productId);
}
