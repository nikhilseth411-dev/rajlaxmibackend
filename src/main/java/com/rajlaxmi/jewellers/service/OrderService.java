package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.CreateOrderRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CouponResponse;
import com.rajlaxmi.jewellers.dto.response.OrderResponse;
import com.rajlaxmi.jewellers.dto.response.PagedResponse;
import com.rajlaxmi.jewellers.enums.OrderStatus;

import java.math.BigDecimal;

public interface OrderService {
    ApiResponse<OrderResponse> placeOrder(Long userId, CreateOrderRequest request);
    OrderResponse getOrderById(Long orderId, Long userId);
    OrderResponse getOrderByNumber(String orderNumber);
    PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size);
    ApiResponse<OrderResponse> cancelOrder(Long orderId, Long userId);
    // Admin
    PagedResponse<OrderResponse> getAllOrders(int page, int size);
    ApiResponse<OrderResponse> updateOrderStatus(Long orderId, OrderStatus status, String adminNote);
    ApiResponse<OrderResponse> updatePaymentStatus(Long orderId, String transactionId);
    // Coupon
    ApiResponse<CouponResponse> validateCoupon(String code, BigDecimal orderTotal);
}
