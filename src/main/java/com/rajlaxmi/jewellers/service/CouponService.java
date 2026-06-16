package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.CouponRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CouponResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    ApiResponse<CouponResponse> validateCoupon(String code, BigDecimal orderTotal);

    // Admin
    ApiResponse<CouponResponse> createCoupon(CouponRequest request);

    ApiResponse<CouponResponse> updateCoupon(Long id, CouponRequest request);

    ApiResponse<String> deleteCoupon(Long id);

    List<CouponResponse> getAllCoupons();
}
