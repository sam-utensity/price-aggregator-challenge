package com.samdoherty.aggregator.infrastructure.restclient.bitstamp.client;

import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Market;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class TestableBitstampApiClient extends BitstampApiClient {
    public TestableBitstampApiClient(RestClient restClient) {
        super("http://dummy-url");
        try {
            java.lang.reflect.Field field = BitstampApiClient.class.getDeclaredField("restClient");
            field.setAccessible(true);
            field.set(this, restClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class BitstampApiClientTest {
    private RestClient restClient;
    private BitstampApiClient apiClient;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        apiClient = new TestableBitstampApiClient(restClient);
    }

    @Test
    void testGetMarkets() {
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        List<Market> mockMarkets = Arrays.asList(new Market("ab", "a", "b", 1), new Market("cd", "c", "d", 1));

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v2/markets/")).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(mockMarkets);

        List<Market> result = apiClient.getMarkets();
        assertEquals(2, result.size());
    }

    @Test
    void testGetLatestPrice() {
        String symbol = "btcusd";
        RequestHeadersUriSpec uriSpec = mock(RequestHeadersUriSpec.class);
        RequestHeadersSpec headersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        Ticker mockTicker = new Ticker(BigDecimal.TEN);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v2/ticker/" + symbol)).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Ticker.class)).thenReturn(mockTicker);

        Ticker result = apiClient.getLatestPrice(symbol);
        assertNotNull(result);
    }
} 