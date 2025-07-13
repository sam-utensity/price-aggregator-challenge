package com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto;

import java.math.BigDecimal;

public record TradeData(
        /*
         * Literally all we need...
         *
         * Timestamp we use system time to ensure any drift between other exchanges does not skew data
         */
        BigDecimal price
) {
}
