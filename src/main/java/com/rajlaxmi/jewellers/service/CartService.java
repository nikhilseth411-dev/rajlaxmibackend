package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.AddToCartRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    ApiResponse<CartResponse> addToCart(Long userId, AddToCartRequest request);
    ApiResponse<CartResponse> updateQuantity(Long userId, Long productId, int quantity);
    ApiResponse<String> removeFromCart(Long userId, Long productId);
    ApiResponse<String> clearCart(Long userId);
    int getCartItemCount(Long userId);
}
