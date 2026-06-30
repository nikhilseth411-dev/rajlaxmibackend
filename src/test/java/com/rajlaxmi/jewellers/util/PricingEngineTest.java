package com.rajlaxmi.jewellers.util;

import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.entity.Product;
import com.rajlaxmi.jewellers.enums.GoldPurity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PricingEngineTest {

    private final PricingEngine pricingEngine = new PricingEngine();
    private final GoldPrice goldPrice = GoldPrice.builder()
            .rate24K(new BigDecimal("8000.00"))
            .rate22K(new BigDecimal("7300.00"))
            .rate18K(new BigDecimal("6000.00"))
            .build();

    @Test
    void calculatesFixedMakingCharges() {
        assertThat(calculateMakingCharges("FIXED", "500.00"))
                .isEqualByComparingTo("500.00");
    }

    @Test
    void calculatesPerGramMakingCharges() {
        assertThat(calculateMakingCharges("PER_GRAM", "200.00"))
                .isEqualByComparingTo("2000.00");
    }

    @Test
    void calculatesPercentageMakingChargesFromBaseMetalValue() {
        assertThat(calculateMakingCharges("PERCENTAGE", "10.00"))
                .isEqualByComparingTo("7300.00");
    }

    private BigDecimal calculateMakingCharges(String type, String amount) {
        Product product = Product.builder()
                .name("Test Product")
                .weightGrams(new BigDecimal("10.00"))
                .goldPurity(GoldPurity.GOLD_22K)
                .makingCharges(new BigDecimal(amount))
                .makingChargesType(type)
                .stoneCharges(BigDecimal.ZERO)
                .gstPercentage(BigDecimal.ZERO)
                .build();

        return pricingEngine.calculatePrice(product, goldPrice).getMakingCharges();
    }
}
