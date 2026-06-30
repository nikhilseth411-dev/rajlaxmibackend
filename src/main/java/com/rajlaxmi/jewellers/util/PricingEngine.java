package com.rajlaxmi.jewellers.util;

import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.enums.GoldPurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ================================================================
 * PricingEngine — Live Jewellery Price Calculator
 * ================================================================
 * This is the most critical business logic component.
 *
 * FORMULA:
 *   ┌────────────────────────────────────────────────────────────┐
 *   │ Base Metal Value = Weight(g) × Live Gold Rate(₹/g)        │
 *   │                    × (Purity% / 100)                      │
 *   │                                                            │
 *   │ Making Charges = flat ₹/g × weight  OR  fixed ₹ amount   │
 *   │                                                            │
 *   │ Stone Charges  = fixed ₹ (for diamonds/stones, if any)   │
 *   │                                                            │
 *   │ Taxable Value  = Base + Making + Stone                    │
 *   │                                                            │
 *   │ GST Amount     = Taxable Value × GST%                     │
 *   │                                                            │
 *   │ FINAL PRICE    = Taxable Value + GST Amount               │
 *   └────────────────────────────────────────────────────────────┘
 *
 * EXAMPLE (22K Gold Necklace, 8.5g):
 *   Live 24K rate = ₹7,200/g
 *   22K rate = ₹7,200 × 0.916 = ₹6,595/g
 *   Base = 8.5g × ₹6,595 = ₹56,058
 *   Making charges = ₹150/g × 8.5g = ₹1,275  (PER_GRAM type)
 *   Stone charges = ₹0
 *   Taxable = ₹57,333
 *   GST @ 3% = ₹1,720
 *   FINAL PRICE = ₹59,053
 *
 * WHY BigDecimal instead of double?
 *   double has floating-point precision errors:
 *   0.1 + 0.2 = 0.30000000000000004 (wrong!)
 *   BigDecimal gives exact decimal arithmetic — essential for money.
 *   We use HALF_UP rounding (standard rounding for currency).
 * ================================================================
 */
@Component
@Slf4j
public class PricingEngine {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Calculates the final selling price for a product.
     *
     * @param product   the jewellery product with weight, purity, charges
     * @param goldPrice current live gold price (24K rate per gram)
     * @return PriceBreakdown containing all components and final price
     */
    public PriceBreakdown calculatePrice(Product product, GoldPrice goldPrice) {
        // ── Step 1: Determine gold rate based on purity ───────
        BigDecimal goldRatePerGram = getGoldRateForPurity(product.getGoldPurity(), goldPrice);

        // ── Step 2: Calculate base metal value ────────────────
        BigDecimal baseMetalValue = product.getWeightGrams()
                .multiply(goldRatePerGram)
                .setScale(SCALE, ROUNDING);

        // ── Step 3: Calculate making charges ──────────────────
        BigDecimal makingChargesTotal = calculateMakingCharges(product, baseMetalValue);

        // ── Step 4: Stone/diamond charges (fixed per product) ─
        BigDecimal stoneCharges = product.getStoneCharges() != null
                ? product.getStoneCharges().setScale(SCALE, ROUNDING)
                : BigDecimal.ZERO;

        // ── Step 5: Taxable value (before GST) ────────────────
        BigDecimal taxableValue = baseMetalValue
                .add(makingChargesTotal)
                .add(stoneCharges);

        // ── Step 6: GST calculation ────────────────────────────
        BigDecimal gstPercent = product.getGstPercentage() != null
                ? product.getGstPercentage()
                : new BigDecimal("3.00");

        BigDecimal gstAmount = taxableValue
                .multiply(gstPercent)
                .divide(new BigDecimal("100"), SCALE, ROUNDING);

        // ── Step 7: Final price ────────────────────────────────
        BigDecimal finalPrice = taxableValue.add(gstAmount);

        log.debug("Price calculated for product '{}': ₹{}", product.getName(), finalPrice);

        return PriceBreakdown.builder()
                .productId(product.getId())
                .productName(product.getName())
                .weightGrams(product.getWeightGrams())
                .goldPurity(product.getGoldPurity())
                .goldRatePerGram(goldRatePerGram)
                .baseMetalValue(baseMetalValue)
                .makingCharges(makingChargesTotal)
                .stoneCharges(stoneCharges)
                .taxableValue(taxableValue)
                .gstPercentage(gstPercent)
                .gstAmount(gstAmount)
                .finalPrice(finalPrice)
                .build();
    }

    /**
     * Gets the appropriate gold rate for the given purity.
     *
     * The API returns 24K rate. We derive other purities:
     *   22K = 24K × 0.916
     *   18K = 24K × 0.750
     *
     * We use the pre-calculated values stored in GoldPrice entity
     * (calculated when the API data was saved) for consistency.
     */
    private BigDecimal getGoldRateForPurity(GoldPurity purity, GoldPrice goldPrice) {
        if (purity == null) {
            // Default to 22K for unspecified purity
            return goldPrice.getRate22K();
        }
        return switch (purity) {
            case GOLD_18K -> goldPrice.getRate18K();
            case GOLD_22K -> goldPrice.getRate22K();
            case GOLD_24K -> goldPrice.getRate24K();
        };
    }

    /**
     * Calculates making charges based on the charge type.
     *
     * PER_GRAM: makingCharges (₹/g) × weightGrams
     * FIXED:    makingCharges as flat ₹ amount
     */
    private BigDecimal calculateMakingCharges(Product product, BigDecimal baseMetalValue) {
        if (product.getMakingCharges() == null) return BigDecimal.ZERO;

        if ("PERCENTAGE".equals(product.getMakingChargesType())) {
            return baseMetalValue
                    .multiply(product.getMakingCharges())
                    .divide(new BigDecimal("100"), SCALE, ROUNDING);
        }

        if ("PER_GRAM".equals(product.getMakingChargesType())) {
            return product.getMakingCharges()
                    .multiply(product.getWeightGrams())
                    .setScale(SCALE, ROUNDING);
        } else {
            // FIXED — flat amount for the entire piece
            return product.getMakingCharges().setScale(SCALE, ROUNDING);
        }
    }

    /**
     * ============================================================
     * PriceBreakdown — immutable value object for price details
     * ============================================================
     * Sent in API responses so frontend can display:
     *   "Base Metal Value: ₹56,058"
     *   "Making Charges: ₹1,275"
     *   "GST (3%): ₹1,720"
     *   "Total: ₹59,053"
     *
     * This transparency is a trust signal for jewellery buyers.
     * ============================================================
     */
    @lombok.Builder
    @lombok.Getter
    public static class PriceBreakdown {
        private Long productId;
        private String productName;
        private BigDecimal weightGrams;
        private GoldPurity goldPurity;
        private BigDecimal goldRatePerGram;
        private BigDecimal baseMetalValue;
        private BigDecimal makingCharges;
        private BigDecimal stoneCharges;
        private BigDecimal taxableValue;
        private BigDecimal gstPercentage;
        private BigDecimal gstAmount;
        private BigDecimal finalPrice;
    }
}
