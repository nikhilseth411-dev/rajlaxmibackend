package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.entity.SilverPrice;

import java.math.BigDecimal;

public interface SilverPriceService {

    SilverPrice getCurrentSilverPriceEntity();

    ApiResponse<BigDecimal> getCurrentSilverRate();
}
