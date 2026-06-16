package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.CouponRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.CouponResponse;
import com.rajlaxmi.jewellers.entity.Coupon;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.DuplicateResourceException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.CouponRepository;
import com.rajlaxmi.jewellers.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CouponResponse> validateCoupon(String code, BigDecimal orderTotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Invalid coupon code: " + code));

        if (!coupon.isValid()) {
            throw new BusinessException("Coupon '" + code + "' is expired or no longer valid.");
        }
        if (orderTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BusinessException("Minimum order amount of ₹" +
                    coupon.getMinimumOrderAmount() + " required for this coupon.");
        }

        BigDecimal discount = coupon.calculateDiscount(orderTotal);
        CouponResponse response = toResponse(coupon);
        response.setApplicableDiscountAmount(discount);
        return ApiResponse.success("Coupon applied! You save ₹" + discount, response);
    }

    @Override
    public ApiResponse<CouponResponse> createCoupon(CouponRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new DuplicateResourceException("Coupon", "code", request.getCode());
        }
        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minimumOrderAmount(request.getMinimumOrderAmount() != null
                        ? request.getMinimumOrderAmount() : BigDecimal.ZERO)
                .usageLimit(request.getUsageLimit())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .isActive(true)
                .build();
        couponRepository.save(coupon);
        return ApiResponse.success("Coupon created.", toResponse(coupon));
    }

    @Override
    public ApiResponse<CouponResponse> updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        if (request.getDescription() != null) coupon.setDescription(request.getDescription());
        if (request.getDiscountValue() != null) coupon.setDiscountValue(request.getDiscountValue());
        if (request.getMaxDiscountAmount() != null) coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getMinimumOrderAmount() != null) coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) coupon.setValidUntil(request.getValidUntil());
        couponRepository.save(coupon);
        return ApiResponse.success("Coupon updated.", toResponse(coupon));
    }

    @Override
    public ApiResponse<String> deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        coupon.setActive(false);
        couponRepository.save(coupon);
        return ApiResponse.success("Coupon deactivated.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CouponResponse toResponse(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .discountType(c.getDiscountType())
                .discountValue(c.getDiscountValue())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .minimumOrderAmount(c.getMinimumOrderAmount())
                .usageLimit(c.getUsageLimit())
                .usedCount(c.getUsedCount())
                .validFrom(c.getValidFrom())
                .validUntil(c.getValidUntil())
                .isActive(c.isActive())
                .isValid(c.isValid())
                .build();
    }
}
