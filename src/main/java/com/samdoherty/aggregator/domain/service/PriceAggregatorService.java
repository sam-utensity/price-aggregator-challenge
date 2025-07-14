package com.samdoherty.aggregator.domain.service;

import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.model.Price;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core aggregation service that exchange websocket implementations register latest exchange currency pair prices
 * <p>
 * Implementation is threadsafe and built for speed
 */
@Service
public class PriceAggregatorService {

    private final Map<Instrument, Price> prices = new ConcurrentHashMap<>();

    /**
     * Simple way to add a starting price for a currency pair
     * <p>
     * To reduce the risk of race conditions, with the exchange websocket prices are only added if no price for the instrument yet exists
     *
     * @param instrument we are targeting
     * @param price      starting price. is ONLY added if no price yet exists for the instrument
     */
    public void addInitialPrice(@NotNull Instrument instrument, @NotNull BigDecimal price) {
        prices.computeIfAbsent(instrument, i -> Price.builder().price(price).time(ZonedDateTime.now()).build());
    }

    public void addPrice(@NotNull Instrument instrument, @NotNull BigDecimal price) {
        prices.put(instrument, Price.builder().price(price).time(ZonedDateTime.now()).build());
    }

    public @Nullable Price getPrice(@NotNull Instrument instrument) {
        return prices.get(instrument);
    }
}
