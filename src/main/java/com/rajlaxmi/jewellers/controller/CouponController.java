package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.CouponRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CouponResponse;
import com.rajlaxmi.jewellers.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon validation and admin coupon management")
@SecurityRequirement(name = "BearerAuth")
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    @Operation(summary = "Validate coupon for checkout")
    public ResponseEntity<ApiResponse<CouponResponse>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) {

        return ResponseEntity.ok(couponService.validateCoupon(code, orderTotal));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all coupons")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons()));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Create coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CouponRequest request) {

        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {

        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Deactivate coupon")
    public ResponseEntity<ApiResponse<String>> deleteCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.deleteCoupon(id));
    }
}