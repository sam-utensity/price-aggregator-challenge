package com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Market(
        @JsonProperty("market_symbol")
        String symbol,
        @JsonProperty("base_currency")
        String base,
        @JsonProperty("counter_currency")
        String quote,
        @JsonProperty("instant_order_counter_decimals")
        int quotePriceDecimals
) {
}
