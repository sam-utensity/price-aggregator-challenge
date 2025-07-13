package com.samdoherty.aggregator.infrastructure.configuration;

import jakarta.validation.constraints.Pattern;
import org.jetbrains.annotations.NotNull;

public record Pair(
        /*
         * Enforced uppercase is a little strict, but it enforces consistence and removes human error when implementing exchanges
         */
        @Pattern(regexp = "^[A-Z]{3}$", message = "Pair property 'base' must be in the format 'AAA' using uppercase letters only.")
        String base,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Pair property 'quote' must be in the format 'AAA' using uppercase letters only.")
        String quote
) {
    @Override
    public @NotNull String toString() {
        return "%s/%s".formatted(base, quote);
    }
}
