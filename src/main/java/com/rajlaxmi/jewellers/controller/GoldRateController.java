package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.GoldRateResponse;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gold-rates")
@RequiredArgsConstructor
@Tag(name = "Gold Rates", description = "Live gold and silver prices")
public class GoldRateController {

    private final GoldPriceService goldPriceService;

    @GetMapping("/current")
    @Operation(summary = "Get current live gold and silver rates")
    public ResponseEntity<ApiResponse<GoldRateResponse>> getCurrentRates() {
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getCurrentRates()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get gold rate history with chart data")
    public ResponseEntity<ApiResponse<GoldRateResponse>> getRateHistory(
            @RequestParam(defaultValue = "30") int points) {
        return ResponseEntity.ok(ApiResponse.success(goldPriceService.getCurrentRatesWithHistory(points)));
    }
}
