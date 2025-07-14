package com.samdoherty.aggregator.infrastructure.websocket.bitstamp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.service.PriceAggregatorService;
import com.samdoherty.aggregator.infrastructure.configuration.Pair;
import com.samdoherty.aggregator.infrastructure.configuration.PairsConfiguration;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.client.BitstampApiClient;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Market;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Ticker;
import com.samdoherty.aggregator.infrastructure.websocket.AbstractExchangeWebsocket;
import com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto.Event;
import com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto.SubscribeMessage;
import com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto.TradeData;
import com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto.WebsocketMessage;
import jakarta.websocket.ClientEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Core websocket logic for consuming market data from bitstamp
 */
@Slf4j
@Service
@ClientEndpoint
@ConditionalOnProperty(prefix = "exchange.bitstamp", name = "websocketUrl", matchIfMissing = false)
public class BitstampWebsocket extends AbstractExchangeWebsocket {

    @Getter
    private final String name;

    @Getter
    private final List<Pair> pairs;

    /**
     * Bitstamp channel to normalized Instrument object mappings
     * <p>
     * Used as a lightweight adapter
     */
    private final Map<String, Instrument> channelToInstrumentMap = new HashMap<>();

    private final PriceAggregatorService aggregatorService;
    private final BitstampApiClient apiClient;

    private static final String CHANNEL_PREFIX = "live_trades_";

    /**
     * A bit ugly, but the design of the websocket makes it a bit tricky without cleaver parsing generics
     */
    private static final String MESSAGE_TRADE_STRING = "\"event\":\"trade\"";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BitstampWebsocket(
            @Value("${exchange.bitstamp.name}") String name,
            @Value("${exchange.bitstamp.websocketUrl}") String websocketURL,
            PairsConfiguration pairsConfiguration,
            PriceAggregatorService aggregatorService,
            BitstampApiClient apiClient
    ) {
        super(websocketURL);
        this.name = name;
        this.pairs = pairsConfiguration.pairs();
        this.aggregatorService = aggregatorService;
        this.apiClient = apiClient;

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Added these here as an easy way to ensure the application fails to start upon error
        // As opposed to a zombie service
        mapSymbolsToInstruments();
        connect();
    }

    /**
     * Inplace of an adaptor layer, we simply generate mappings for currency pairs
     * <p>
     * This method compares configured currency pairs with Bitstamp markets
     * <p>
     * This allows us to map websocket channels to normalized Instrument objects by which we store and retrieve pricing
     */
    private void mapSymbolsToInstruments() {
        List<Market> allMarkets = apiClient.getMarkets();

        for (Pair pair : pairs) {

            String pairSymbol = pairToSymbol(pair);

            Market market = allMarkets.stream()
                    .filter(m -> Objects.equals(pairSymbol, m.symbol())).findFirst()
                    .orElseThrow(() -> new RuntimeException("Base '%s' - Quote '%s' does not appear to be an available bitstamp market".formatted(pair.base(), pair.quote())));

            channelToInstrumentMap.put(CHANNEL_PREFIX + pairSymbol, Instrument.builder()
                    .exchange("bitstamp")
                    .base(pair.base())
                    .quote(pair.quote())
                    .scale(market.quotePriceDecimals()).build());
        }
    }

    /**
     * Map a configured currency pair to a Bitstamp compatible market symbol
     *
     * @param pair configured for monitoring
     * @return string symbol
     */
    private String pairToSymbol(Pair pair) {
        return "%s%s".formatted(pair.base(), pair.quote()).toLowerCase();
    }

    @Override
    public void subscribe() {
        for (String channel : channelToInstrumentMap.keySet()) {
            try {
                sendMessage(WebsocketMessage.builder()
                        .event(Event.SUBSCRIBE)
                        .data(SubscribeMessage.builder()
                                .channel(channel).build()).build());

                prePopulatePrices(channel);
            } catch (IOException e) {
                throw new RuntimeException("Unable to subscribe to bitstamp channel %s".formatted(channel), e);
            }
        }
    }

    @Override
    public void healthCheck() {
        try {
            sendMessage(WebsocketMessage.builder().event(Event.HEARTBEAT).build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A little sugar beyond the spec. Rather than wait for a first trade via the websocket,
     * we proactively retrieve the latest price and apply to the aggregator service (write if not null)
     * <p>
     * To guarantee the avoidance of race conditions, we would ideally hold websocket messages until this has completed
     */
    private void prePopulatePrices(String channel) {
        Instrument instrument = channelToInstrumentMap.get(channel);

        Ticker ticker = apiClient.getLatestPrice(channel.replace(CHANNEL_PREFIX, ""));

        aggregatorService.addInitialPrice(instrument, ticker.last().setScale(instrument.getScale(), RoundingMode.HALF_EVEN));
    }

    @Override
    public void readMessage(@NotNull String message) {

        if (message.contains(MESSAGE_TRADE_STRING)) {
            processTradeMessage(message);
            return;
        }

        if (message.contains(Event.SUBSCRIBE_SUCCESS.toString())) {
            return;
        }

        if (message.contains(Event.RECONNECT_REQUEST.toString())) {
            close(); // Close and allow the restart mechanism to take over
            return;
        }

        if (message.contains(Event.HEARTBEAT.toString())) {
            return;
        }

        log.info("Received additional message: {}", message);
    }

    private void processTradeMessage(@NotNull String message) {
        try {
            WebsocketMessage<TradeData> trade = objectMapper.readValue(message, new TypeReference<>() {
            });
            Instrument instrument = channelToInstrumentMap.get(trade.channel());
            aggregatorService.addPrice(instrument, trade.data().price().setScale(instrument.getScale(), RoundingMode.HALF_EVEN));

            log.debug("Received trade data: {}", trade);
        } catch (IOException e) {
            log.error("Unable to parse message from bitstamp channel {}", message, e);
        }
    }
}
