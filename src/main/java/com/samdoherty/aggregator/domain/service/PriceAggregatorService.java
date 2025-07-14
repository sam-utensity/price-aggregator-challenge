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


@Service
public class PriceAggregatorService {

    private final Map<Instrument, Price> prices = new ConcurrentHashMap<>();

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
