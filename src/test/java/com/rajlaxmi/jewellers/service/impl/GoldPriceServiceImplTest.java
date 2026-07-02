package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.GoldPriceOverrideRequest;
import com.rajlaxmi.jewellers.entity.GoldPrice;
import com.rajlaxmi.jewellers.repository.GoldPriceRepository;
import com.rajlaxmi.jewellers.repository.SilverPriceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldPriceServiceImplTest {
    @Test
    void savesAllAdminProvidedPurityRatesAsCurrent() {
        GoldPriceRepository repository = mock(GoldPriceRepository.class);
        GoldPriceServiceImpl service = new GoldPriceServiceImpl(repository, mock(SilverPriceRepository.class));
        GoldPriceOverrideRequest request = new GoldPriceOverrideRequest();
        request.setRate24KPerGram(new BigDecimal("7500"));
        request.setRate22KPerGram(new BigDecimal("6870"));
        request.setRate18KPerGram(new BigDecimal("5625"));
        when(repository.save(any(GoldPrice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByIsCurrentTrue()).thenAnswer(invocation -> Optional.of(
                GoldPrice.builder()
                        .rate24K(new BigDecimal("7500"))
                        .rate22K(new BigDecimal("6870"))
                        .rate18K(new BigDecimal("5625"))
                        .isCurrent(true)
                        .isAdminOverride(true)
                        .build()
        ));

        service.adminOverridePrice(request);

        verify(repository).markAllAsNotCurrent();
        verify(repository).save(any(GoldPrice.class));
    }

    @Test
    void reportsClearMessageWhenNoRateIsConfigured() {
        GoldPriceRepository repository = mock(GoldPriceRepository.class);
        when(repository.findByIsCurrentTrue()).thenReturn(Optional.empty());
        GoldPriceServiceImpl service = new GoldPriceServiceImpl(repository, mock(SilverPriceRepository.class));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(service::getCurrentGoldPriceEntity))
                .hasMessage("Gold rate is not configured yet. Please ask admin to update gold rate.");
    }
}
