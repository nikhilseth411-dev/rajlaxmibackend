package com.rajlaxmi.jewellers.scheduler;

import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.repository.GoldPriceRepository;
import com.rajlaxmi.jewellers.repository.SilverPriceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void neverOverwritesActiveAdminRate() {
        GoldPriceRepository goldRepository = mock(GoldPriceRepository.class);
        GoldPriceScheduler scheduler = new GoldPriceScheduler(
                goldRepository,
                mock(SilverPriceRepository.class),
                mock(CacheManager.class),
                mock(WebClient.Builder.class)
        );
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
        when(goldRepository.findByIsCurrentTrue()).thenReturn(Optional.of(
                GoldPrice.builder().isCurrent(true).isAdminOverride(true).build()
        ));

        scheduler.fetchAndUpdateGoldPrices();

        verify(goldRepository, never()).markAllAsNotCurrent();
    }
}
