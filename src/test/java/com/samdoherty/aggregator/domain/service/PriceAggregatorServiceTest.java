package com.samdoherty.aggregator.domain.service;

import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.model.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PriceAggregatorServiceTest {
    private PriceAggregatorService service;
    private Instrument instrument;

    @BeforeEach
    void setUp() {
        service = new PriceAggregatorService();
        instrument = Instrument.builder()
                .exchange("bitstamp")
                .base("BTC")
                .quote("USD")
                .scale(2)
                .build();
    }

    @Test
    void addInitialPrice_shouldAddPriceIfAbsent() {
        service.addInitialPrice(instrument, BigDecimal.TEN);
        Price price = service.getPrice(instrument);
        assertNotNull(price);
        assertEquals(BigDecimal.TEN, price.price());
    }

    @Test
    void addInitialPrice_shouldReturnIfNoScale() {
        service.addInitialPrice(instrument, BigDecimal.ONE);
        Price price = service.getPrice(Instrument.builder()
                .exchange("bitstamp")
                .base("BTC")
                .quote("USD")
                .build());
        assertNotNull(price);
        assertEquals(BigDecimal.ONE, price.price());
    }

    @Test
    void addInitialPrice_shouldReturnIfNotBitstamp() {
        service.addInitialPrice(Instrument.builder()
                .exchange("coinbase")
                .base("BTC")
                .quote("USD")
                .scale(6)
                .build(), BigDecimal.TWO);
        Price price = service.getPrice(Instrument.builder()
                .exchange("coinbase")
                .base("BTC")
                .quote("USD")
                .build());
        assertNotNull(price);
        assertEquals(BigDecimal.TWO, price.price());
    }

    @Test
    void addInitialPrice_shouldNotOverwriteExistingPrice() {
        service.addInitialPrice(instrument, BigDecimal.TEN);
        service.addInitialPrice(instrument, BigDecimal.ONE);
        Price price = service.getPrice(instrument);
        assertNotNull(price);
        assertEquals(BigDecimal.TEN, price.price());
    }

    @Test
    void addPrice_shouldOverwriteExistingPrice() {
        service.addInitialPrice(instrument, BigDecimal.TEN);
        service.addPrice(instrument, BigDecimal.ONE);
        Price price = service.getPrice(instrument);
        assertNotNull(price);
        assertEquals(BigDecimal.ONE, price.price());
    }

    @Test
    void getPrice_shouldReturnNullIfNotPresent() {
        Price price = service.getPrice(instrument);
        assertNull(price);
    }
} 