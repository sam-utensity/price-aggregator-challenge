package com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto;

import java.math.BigDecimal;

public record Ticker(
        // All we need
        BigDecimal last
) {
}
