package com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto;

import java.math.BigDecimal;

/**
 * Bitstamp ticker data for a particular instrument
 * <p>
 * NOTE: We only parse last (price) as this is all we currently need
 */
public record Ticker(
        // All we need
        BigDecimal last
) {
}
