package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.entity.SilverPrice;
import com.rajlaxmi.jewellers.repository.SilverPriceRepository;
import com.rajlaxmi.jewellers.service.SilverPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SilverPriceServiceImpl implements SilverPriceService {

    private final SilverPriceRepository silverPriceRepository;

    @Override
    public SilverPrice getCurrentSilverPriceEntity() {
        return silverPriceRepository.findByIsCurrentTrue().orElse(null);
    }

    @Override
    public ApiResponse<BigDecimal> getCurrentSilverRate() {
        SilverPrice price = getCurrentSilverPriceEntity();
        if (price == null) {
            return ApiResponse.success("Silver price not available.", null);
        }
        return ApiResponse.success(price.getRatePerGram());
    }
}
