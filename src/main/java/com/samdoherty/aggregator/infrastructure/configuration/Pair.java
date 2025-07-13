package com.samdoherty.aggregator.infrastructure.configuration;

import org.jetbrains.annotations.NotNull;

public record Pair(
        String base,
        String quote
) {
    @Override
    public @NotNull String toString() {
        return "%s/%s".formatted(base, quote);
    }
}
