package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.CreateOrderRequest;
import com.rajlaxmi.jewellers.dto.response.*;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.OrderStatus;
import com.rajlaxmi.jewellers.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, tracking, and management")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order from current cart")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(user.getId(), request));
    }

    @GetMapping
    @Operation(summary = "Get current user's order history")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(user.getId(), page, size)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order detail by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(orderId, user.getId())));
    }

    @GetMapping("/track/{orderNumber}")
    @Operation(summary = "Track order by order number (e.g. RLJ-2025-000001)")
    public ResponseEntity<ApiResponse<OrderResponse>> trackOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByNumber(orderNumber)));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order (only if not yet shipped)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user.getId()));
    }

    // ── Coupon ────────────────────────────────────────────────

    @GetMapping("/coupon/validate")
    @Operation(summary = "Validate coupon code and calculate discount")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) {
        return ResponseEntity.ok(orderService.validateCoupon(code, orderTotal));
    }

    // ── Admin Order Management ─────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(page, size)));
    }

    @PutMapping("/admin/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, note));
    }

    @PutMapping("/admin/{orderId}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Mark order payment as verified (UPI/COD)")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(
            @PathVariable Long orderId,
            @RequestParam String transactionId,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(orderService.updatePaymentStatus(orderId, transactionId, admin.getEmail()));
    }
}
