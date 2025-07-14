package com.samdoherty.aggregator.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Simple price response object for API
 * @param price
 * @param time
 */
@Builder
public record Price(
        BigDecimal price,
        /*
         * Much easier to ensure ISO 8601 using ZonedDateTime
         */
        ZonedDateTime time
) {
}
