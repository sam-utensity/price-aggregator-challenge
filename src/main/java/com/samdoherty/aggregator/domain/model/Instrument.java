package com.samdoherty.aggregator.domain.model;

import lombok.Builder;

@Builder
public record Instrument(
        String exchange,
        String base,
        String quote
) {
}
