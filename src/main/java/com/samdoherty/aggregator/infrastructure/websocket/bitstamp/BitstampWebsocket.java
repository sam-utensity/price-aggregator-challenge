package com.samdoherty.aggregator.infrastructure.websocket.bitstamp;


import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.service.PriceAggregatorService;
import com.samdoherty.aggregator.infrastructure.configuration.Pair;
import com.samdoherty.aggregator.infrastructure.configuration.PairsConfiguration;
import com.samdoherty.aggregator.infrastructure.websocket.AbstractExchangeWebsocket;
import jakarta.websocket.ClientEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ClientEndpoint
@ConditionalOnProperty(prefix = "exchange.bitstamp", name = "websocketUrl", matchIfMissing = false)
public class BitstampWebsocket extends AbstractExchangeWebsocket {

    @Getter
    private final String name;

    @Getter
    private final List<Pair> pairs;

    private Map<String, Instrument> exchangeSymbolInstrumentMap;

    private final PriceAggregatorService aggregatorService;

    public BitstampWebsocket(
            @Value("${exchange.bitstamp.name}") String name,
            @Value("${exchange.bitstamp.websocketUrl}") String websocketURL,
            PairsConfiguration pairsConfiguration,
            PriceAggregatorService aggregatorService
    ) {
        super(websocketURL);
        this.name = name;
        this.pairs = pairsConfiguration.pairs();
        this.aggregatorService = aggregatorService;
    }

    @Override
    public void subscribe() {
        exchangeSymbolInstrumentMap = new HashMap<>();
        exchangeSymbolInstrumentMap.put("", Instrument.builder()
                .exchange("bitstamp")
                .base("")
                .quote("").build());
    }

    @Override
    public void readMessage(@NotNull String message) {

        Instrument instrument = exchangeSymbolInstrumentMap.get(message);

        aggregatorService.addPrice(instrument, BigDecimal.ZERO);
    }
}
