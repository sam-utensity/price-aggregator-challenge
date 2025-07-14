package com.samdoherty.aggregator.infrastructure.restclient.bitstamp.client;

import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Market;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Ticker;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Simple API connection to Bitstamp
 */
@Service
public class BitstampApiClient {

    private final RestClient restClient;

    public BitstampApiClient(@Value("${exchange.bitstamp.apiUrl}") String url) {
        restClient = RestClient.create(url);
    }

    /**
     * Get all available markets Bitstamp supports
     *
     * @return list of markets
     */
    public List<Market> getMarkets() {
        return restClient.get()
                .uri("/api/v2/markets/")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    /**
     * Get the latest ticket containing the latest price
     *
     * @param symbol to get the latest tick data for
     * @return the latest tick data
     */
    public Ticker getLatestPrice(@NotNull String symbol) {
        return restClient.get()
                .uri("/api/v2/ticker/" + symbol)
                .retrieve()
                .body(Ticker.class);
    }
}
