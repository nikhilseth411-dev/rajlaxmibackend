package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.GoldPriceOverrideRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.GoldRateResponse;
import com.rajlaxmi.jewellers.entity.GoldPrice;

public interface GoldPriceService {
    GoldRateResponse getCurrentRates();
    GoldRateResponse getCurrentRatesWithHistory(int historyPoints);
    GoldPrice getCurrentGoldPriceEntity();
    ApiResponse<GoldRateResponse> adminOverridePrice(GoldPriceOverrideRequest request);
    ApiResponse<String> removeAdminOverride();
}
