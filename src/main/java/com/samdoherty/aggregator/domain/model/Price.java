package com.samdoherty.aggregator.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record Price(
        BigDecimal price,
        LocalDateTime time
) {
}
