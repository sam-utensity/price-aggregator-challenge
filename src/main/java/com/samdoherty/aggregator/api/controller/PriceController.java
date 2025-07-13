package com.samdoherty.aggregator.api.controller;

import com.samdoherty.aggregator.api.exception.SymbolNotFoundException;
import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.model.Price;
import com.samdoherty.aggregator.domain.service.PriceAggregatorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class PriceController {

    private static final String DEFAULT_EXCHANGE = "bitstamp";

    private final PriceAggregatorService priceAggregatorService;

    @GetMapping("/prices/{symbol}")
    public ResponseEntity<Price> getPrices(
            @Valid
            @NotBlank(message = "Path variable 'symbol' cannot be blank")
            @Pattern(regexp = "^[A-Z]{3}-[A-Z]{3}$", message = "Path variable 'symbol' must be in the format 'AAA-BBB' using uppercase letters only.")
            @PathVariable
            String symbol) {

        Price price = priceAggregatorService.getPrice(Instrument.builder()
                .exchange(DEFAULT_EXCHANGE)
                .base(StringUtils.left(symbol, 3))
                .quote(StringUtils.right(symbol, 3))
                .build());

        if (price == null) {
            throw new SymbolNotFoundException("No symbol found for " + symbol);
        }

        return ResponseEntity.ok(price);

    }
}
