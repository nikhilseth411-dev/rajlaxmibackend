package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.GoldPriceOverrideRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.GoldRateResponse;
import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.SilverPrice;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.GoldPriceRepository;
import com.rajlaxmi.jewellers.repository.SilverPriceRepository;
import com.rajlaxmi.jewellers.service.GoldPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GoldPriceServiceImpl
 *
 * @Cacheable("gold_rates") — caches the result in Redis for 1 hour.
 * When GoldPriceScheduler fetches new prices and evicts the cache,
 * the next call to getCurrentRates() re-fetches from DB and re-caches.
 * This means homepage gold rate requests hit Redis (< 1ms),
 * not the DB, for the entire hour between scheduler runs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoldPriceServiceImpl implements GoldPriceService {

    private final GoldPriceRepository goldPriceRepository;
    private final SilverPriceRepository silverPriceRepository;

    @Override
    @Cacheable(value = "gold_rates", key = "'current'")
    @Transactional(readOnly = true)
    public GoldRateResponse getCurrentRates() {
        return getCurrentRatesWithHistory(0);
    }

    @Override
    @Transactional(readOnly = true)
    public GoldRateResponse getCurrentRatesWithHistory(int historyPoints) {
        GoldPrice goldPrice = goldPriceRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gold rate is not configured yet. Please ask admin to update gold rate."));

        SilverPrice silverPrice = silverPriceRepository.findByIsCurrentTrue().orElse(null);

        List<GoldRateResponse.PriceHistoryPoint> history = List.of();
        if (historyPoints > 0) {
            int safeHistoryPoints = Math.min(historyPoints, 100);
            history = goldPriceRepository
                    .findAllByOrderByFetchedAtDesc(PageRequest.of(0, safeHistoryPoints))
                    .stream()
                    .map(gp -> GoldRateResponse.PriceHistoryPoint.builder()
                            .timestamp(gp.getFetchedAt())
                            .rate24K(gp.getRate24K())
                            .rate22K(gp.getRate22K())
                            .build())
                    .toList();
        }

        return GoldRateResponse.builder()
                .rate24K(goldPrice.getRate24K())
                .rate22K(goldPrice.getRate22K())
                .rate18K(goldPrice.getRate18K())
                .silverRatePerGram(silverPrice != null ? silverPrice.getRatePerGram() : null)
                .goldChangeAmount(goldPrice.getChangeAmount())
                .goldChangePercent(goldPrice.getChangePercent())
                .goldPriceUp(goldPrice.getChangeAmount() != null &&
                             goldPrice.getChangeAmount().compareTo(BigDecimal.ZERO) > 0)
                .lastUpdated(goldPrice.getFetchedAt())
                .source(goldPrice.getSource())
                .isAdminOverride(goldPrice.isAdminOverride())
                .history(history)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GoldPrice getCurrentGoldPriceEntity() {
        return goldPriceRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gold rate is not configured yet. Please ask admin to update gold rate."));
    }

    @Override
    @CacheEvict(value = "gold_rates", allEntries = true)
    @Transactional
    public ApiResponse<GoldRateResponse> adminOverridePrice(GoldPriceOverrideRequest request) {
        // Keep compatibility with older clients that only submit 24K.
        BigDecimal rate24K = request.getRate24KPerGram();
        BigDecimal rate22K = request.getRate22KPerGram() != null
                ? request.getRate22KPerGram()
                : rate24K.multiply(new BigDecimal("0.916")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate18K = request.getRate18KPerGram() != null
                ? request.getRate18KPerGram()
                : rate24K.multiply(new BigDecimal("0.750")).setScale(2, RoundingMode.HALF_UP);

        if (rate24K.compareTo(rate22K) < 0 || rate22K.compareTo(rate18K) < 0) {
            throw new BusinessException("Gold rates must follow 24K >= 22K >= 18K.");
        }

        goldPriceRepository.markAllAsNotCurrent();

        GoldPrice overridePrice = GoldPrice.builder()
                .rate24K(rate24K)
                .rate22K(rate22K)
                .rate18K(rate18K)
                .changeAmount(BigDecimal.ZERO)
                .changePercent(BigDecimal.ZERO)
                .fetchedAt(LocalDateTime.now())
                .currency("INR")
                .source("admin-override")
                .isCurrent(true)
                .isAdminOverride(true)
                .build();

        goldPriceRepository.save(overridePrice);
        log.info("Admin gold rates updated for 24K, 22K and 18K. Reason: {}", request.getReason());

        return ApiResponse.success("Gold price overridden successfully.", getCurrentRates());
    }

    @Override
    @CacheEvict(value = "gold_rates", allEntries = true)
    @Transactional
    public ApiResponse<String> removeAdminOverride() {
        throw new BusinessException("Manual gold rates stay active until an admin saves new rates.");
    }
}
