package com.rajlaxmi.jewellers.scheduler;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoldPriceSchedulerTest {

    @Test
    void extractsNumericPriceFromProviderResponse() {
        BigDecimal price = GoldPriceScheduler.extractPrice(
                Map.of("symbol", "XAU", "price", 4090.6),
                "XAU"
        );

        assertEquals(new BigDecimal("4090.6000"), price);
    }

    @Test
    void rejectsResponseWithoutNumericPrice() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> GoldPriceScheduler.extractPrice(Map.of("symbol", "XAG"), "XAG")
        );

        assertEquals("Missing price for XAG", exception.getMessage());
    }
}
