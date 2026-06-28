package com.rajlaxmi.jewellers.scheduler;

import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.SilverPrice;
import com.rajlaxmi.jewellers.repository.GoldPriceRepository;
import com.rajlaxmi.jewellers.repository.SilverPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * ================================================================
 * GoldPriceScheduler — Automated Live Price Fetching
 * ================================================================
 * WHAT THIS DOES:
 *   Runs every hour to:
 *     1. Fetch current gold/silver prices from gold-api.com
 *     2. Calculate 18K/22K rates from 24K base rate
 *     3. Compute price change vs previous record
 *     4. Save new price record to DB
 *     5. Mark old "current" record as not current
 *     6. Evict Redis cache so next homepage load gets fresh rates
 *
 * gold-api.com API:
 *   URL: https://api.gold-api.com/price/{symbol}
 *   Free, no API key required
 *   Returns: {symbol: "XAU", price: 1930.5, ...} in USD/troy oz
 *
 * CURRENCY CONVERSION:
 *   API returns USD per troy ounce.
 *   1 troy ounce = 31.1035 grams
 *   We fetch USD/INR rate and convert to INR per gram.
 *
 * FALLBACK:
 *   If API call fails (network error, API down):
 *   - Log the error
 *   - Keep existing price in DB (don't update)
 *   - Redis cache still has last valid price
 *   - System continues working with last known price
 *   - Admin is alerted via log (production: send email/alert)
 *
 * @Scheduled cron: "0 0 * * * *"
 *   Second  Minute  Hour  Day  Month  Weekday
 *     0       0      *    *     *       *
 *   = run at :00:00 of every hour
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoldPriceScheduler {

    private final GoldPriceRepository goldPriceRepository;
    private final SilverPriceRepository silverPriceRepository;
    private final CacheManager cacheManager;
    private final WebClient.Builder webClientBuilder;

    @Value("${gold-price.scheduler-enabled:true}")
    private boolean schedulerEnabled;

    @Value("${gold-price.api-url:https://api.gold-api.com}")
    private String metalsApiUrl;

    // Conversion constants
    private static final BigDecimal TROY_OUNCE_TO_GRAMS = new BigDecimal("31.1035");
    private static final int SCALE = 2;
    private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Main scheduled method — runs every hour.
     * cron = "0 0 * * * *" → at second 0, minute 0, every hour
     *
     * Also runs once on startup (initialDelay = 1000ms = 1 second after boot)
     * to ensure prices are loaded when the app first starts.
     */
    @Scheduled(cron = "${gold-price.refresh-cron:0 0 * * * *}")
    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE) // run once on startup
    @Transactional
    public void fetchAndUpdateGoldPrices() {
        if (!schedulerEnabled) {
            log.info("Gold price scheduler is disabled. Using existing or seeded metal rates.");
            return;
        }

        log.info("Starting gold price fetch at {}", LocalDateTime.now());

        try {
            // ── Step 1: Fetch current gold and silver spot prices ──
            MetalsApiResponse apiResponse = fetchFromMetalsApi();

            if (apiResponse == null) {
                log.warn("Metals API returned null response. Keeping existing prices.");
                return;
            }

            // ── Step 2: Convert USD/troy-oz to INR/gram ───────
            BigDecimal usdInrRate = fetchUsdInrRate();
            BigDecimal goldPer24KGramInr = convertToInrPerGram(apiResponse.gold(), usdInrRate);
            BigDecimal silverPerGramInr = convertToInrPerGram(apiResponse.silver(), usdInrRate);

            // ── Step 3: Derive 22K and 18K rates ──────────────
            BigDecimal rate22K = goldPer24KGramInr
                    .multiply(new BigDecimal("0.916"))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal rate18K = goldPer24KGramInr
                    .multiply(new BigDecimal("0.750"))
                    .setScale(SCALE, RoundingMode.HALF_UP);

            // ── Step 4: Calculate price change ────────────────
            Optional<GoldPrice> previousCurrentOpt = goldPriceRepository.findByIsCurrentTrue();

            BigDecimal goldChangeAmount = BigDecimal.ZERO;
            BigDecimal goldChangePercent = BigDecimal.ZERO;

            if (previousCurrentOpt.isPresent()) {
                BigDecimal prevRate = previousCurrentOpt.get().getRate24K();
                goldChangeAmount = goldPer24KGramInr.subtract(prevRate)
                        .setScale(SCALE, RoundingMode.HALF_UP);
                if (prevRate.compareTo(BigDecimal.ZERO) != 0) {
                    goldChangePercent = goldChangeAmount
                            .divide(prevRate, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                }
            }

            // ── Step 5: Save new gold price record ────────────
            // Mark all existing records as not current
            goldPriceRepository.markAllAsNotCurrent();

            GoldPrice newGoldPrice = GoldPrice.builder()
                    .rate24K(goldPer24KGramInr.setScale(SCALE, RoundingMode.HALF_UP))
                    .rate22K(rate22K)
                    .rate18K(rate18K)
                    .changeAmount(goldChangeAmount)
                    .changePercent(goldChangePercent)
                    .fetchedAt(LocalDateTime.now())
                    .currency("INR")
                    .source("gold-api.com")
                    .isCurrent(true)
                    .isAdminOverride(false)
                    .build();

            goldPriceRepository.save(newGoldPrice);

            // ── Step 6: Save silver price ──────────────────────
            silverPriceRepository.markAllAsNotCurrent();

            SilverPrice newSilverPrice = SilverPrice.builder()
                    .ratePerGram(silverPerGramInr.setScale(SCALE, RoundingMode.HALF_UP))
                    .fetchedAt(LocalDateTime.now())
                    .currency("INR")
                    .source("gold-api.com")
                    .isCurrent(true)
                    .build();

            silverPriceRepository.save(newSilverPrice);

            // ── Step 7: Evict Redis cache ─────────────────────
            // Forces next request to fetch fresh prices from DB
            evictGoldRatesCache();

            log.info("Gold prices updated successfully. 24K: ₹{}/g, 22K: ₹{}/g, 18K: ₹{}/g | Silver: ₹{}/g",
                    goldPer24KGramInr, rate22K, rate18K, silverPerGramInr);

        } catch (Exception e) {
            log.error("Failed to fetch gold prices from API: {}. Existing prices remain unchanged.",
                    e.getMessage(), e);
            // Don't rethrow — scheduler must not crash
            // TODO: In production, send alert email to admin
        }
    }

    /**
     * Fetches price data from the configured metals API.
     * Returns null if the API is unreachable or returns invalid data.
     */
    private MetalsApiResponse fetchFromMetalsApi() {
        try {
            BigDecimal gold = fetchMetalPrice("XAU");
            BigDecimal silver = fetchMetalPrice("XAG");
            return new MetalsApiResponse(gold, silver);
        } catch (Exception e) {
            log.error("Failed to call metals price API: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private BigDecimal fetchMetalPrice(String symbol) {
        Map<String, Object> response = webClientBuilder
                .baseUrl(metalsApiUrl)
                .build()
                .get()
                .uri("/price/{symbol}", symbol)
                .retrieve()
                .bodyToMono(Map.class)
                .block(API_TIMEOUT);

        return extractPrice(response, symbol);
    }

    static BigDecimal extractPrice(Map<String, Object> response, String symbol) {
        if (response == null || !(response.get("price") instanceof Number price)) {
            throw new IllegalStateException("Missing price for " + symbol);
        }

        return new BigDecimal(price.toString()).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Fetches current USD to INR exchange rate.
     * Uses a free public API. Fallback to 83.0 if API fails.
     */
    @SuppressWarnings("unchecked")
    private BigDecimal fetchUsdInrRate() {
        try {
            var response = webClientBuilder
                    .baseUrl("https://api.exchangerate-api.com")
                    .build()
                    .get()
                    .uri("/v4/latest/USD")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(API_TIMEOUT);

            if (response != null) {
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                if (rates != null && rates.containsKey("INR")) {
                    return new BigDecimal(rates.get("INR").toString());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch USD/INR rate: {}. Using fallback rate 83.0", e.getMessage());
        }
        return new BigDecimal("83.00"); // fallback rate
    }

    /**
     * Converts price from USD per troy ounce to INR per gram.
     *
     * Formula: (usdPerTroyOz × usdInrRate) / 31.1035
     */
    private BigDecimal convertToInrPerGram(BigDecimal usdPerTroyOz, BigDecimal usdInrRate) {
        return usdPerTroyOz
                .multiply(usdInrRate)
                .divide(TROY_OUNCE_TO_GRAMS, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Evicts the gold_rates cache entries from Redis.
     * Called after successful price update.
     */
    private void evictGoldRatesCache() {
        try {
            var cache = cacheManager.getCache("gold_rates");
            if (cache != null) {
                cache.clear();
                log.debug("Gold rates cache evicted");
            }
        } catch (Exception e) {
            log.warn("Could not evict gold rates cache: {}", e.getMessage());
        }
    }

    /**
     * Internal record to hold parsed API response.
     * Immutable — exactly what a record is designed for.
     */
    private record MetalsApiResponse(BigDecimal gold, BigDecimal silver) {}
}
