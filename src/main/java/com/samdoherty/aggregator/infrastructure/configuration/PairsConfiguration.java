package com.samdoherty.aggregator.infrastructure.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "application")
public record PairsConfiguration(
        @Valid
        @NotEmpty(message = "Ensure there are pairs set in application properties")
        List<Pair> pairs
) {
}
