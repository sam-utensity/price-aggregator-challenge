package com.samdoherty.aggregator.infrastructure.restclient.bitstamp.client;

import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Market;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class BitstampApiClient {

    private final RestClient restClient;

    public BitstampApiClient(@Value("${exchange.bitstamp.apiUrl}") String url) {
        restClient = RestClient.create(url);
    }

    public List<Market> getMarkets() {
        return restClient.get()
                .uri("/api/v2/markets/")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
