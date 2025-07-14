package com.samdoherty.aggregator.infrastructure.websocket.bitstamp;

import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.service.PriceAggregatorService;
import com.samdoherty.aggregator.infrastructure.configuration.Pair;
import com.samdoherty.aggregator.infrastructure.configuration.PairsConfiguration;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.client.BitstampApiClient;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Market;
import com.samdoherty.aggregator.infrastructure.restclient.bitstamp.dto.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BitstampWebsocketTest {

    @Mock
    private PriceAggregatorService aggregatorService;
    @Mock
    private BitstampApiClient apiClient;
    @Mock
    private PairsConfiguration pairsConfiguration;

    private BitstampWebsocket websocket;

    private static final String PAIR_BASE = "BTC";
    private static final String PAIR_QUOTE = "USD";
    private static final String PAIR_SYMBOL = "btcusd";

    @BeforeEach
    void setUp() {
        // Mock pairs
        Pair pair = new Pair(PAIR_BASE, PAIR_QUOTE);
        when(pairsConfiguration.pairs()).thenReturn(List.of(pair));
        // Mock market
        Market market = new Market(PAIR_SYMBOL, PAIR_BASE, PAIR_QUOTE, 2); // 2 decimal places
        when(apiClient.getMarkets()).thenReturn(List.of(market));
        // Mock ticker for prePopulate
        Ticker ticker = mock(Ticker.class);
        // Use the testable subclass to prevent connect() from running
        websocket = new TestableBitstampWebsocket(
                "bitstamp",
                "wss://test",
                pairsConfiguration,
                aggregatorService,
                apiClient
        );
    }

    @Test
    void testReadMessage_withTradeEvent_callsAddPrice() {
        // Given: a trade message JSON
        String tradeMessage = """
                {"event":"trade","channel":"live_trades_btcusd","data":{"price":50001.23}}""";

        // When
        websocket.readMessage(tradeMessage);

        // Then: aggregatorService.addPrice should be called with correct Instrument and price
        ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(aggregatorService, atLeastOnce()).addPrice(instrumentCaptor.capture(), priceCaptor.capture());
        assertEquals("BTC", instrumentCaptor.getValue().getBase());
        assertEquals("USD", instrumentCaptor.getValue().getQuote());
        assertEquals(BigDecimal.valueOf(50001.23).setScale(2), priceCaptor.getValue());
    }

    @Test
    void testReadMessage_withReconnectRequest_callsClose() {
        // Given: a reconnect request message
        String reconnectMessage = "{\"event\":\"bts:request_reconnect\"}";
        // Spy on websocket to verify close is called
        BitstampWebsocket spyWebsocket = spy(websocket);
        // When
        spyWebsocket.readMessage(reconnectMessage);
        // Then
        verify(spyWebsocket, times(1)).close();
    }

    @Test
    void testReadMessage_withHeartbeat_doesNothing() {
        // Given: a heartbeat message
        String heartbeatMessage = "{\"event\":\"bts:heartbeat\"}";
        BitstampWebsocket spyWebsocket = spy(websocket);
        // When
        spyWebsocket.readMessage(heartbeatMessage);
        // Then: aggregatorService and close should not be called
        verifyNoInteractions(aggregatorService);
        verify(spyWebsocket, never()).close();
    }

    @Test
    void testReadMessage_withUnknownEvent_doesNothing() {
        // Given: an unknown event message
        String unknownMessage = "{\"event\":\"something_else\"}";
        BitstampWebsocket spyWebsocket = spy(websocket);
        // When
        spyWebsocket.readMessage(unknownMessage);
        // Then: aggregatorService and close should not be called
        verifyNoInteractions(aggregatorService);
        verify(spyWebsocket, never()).close();
    }

    // Testable subclass to override connect()
    static class TestableBitstampWebsocket extends BitstampWebsocket {
        public TestableBitstampWebsocket(
                String name,
                String websocketURL,
                PairsConfiguration pairsConfiguration,
                PriceAggregatorService aggregatorService,
                BitstampApiClient apiClient
        ) {
            super(name, websocketURL, pairsConfiguration, aggregatorService, apiClient);
        }

        @Override
        protected void connect() {
            // Do nothing to prevent real connection during tests
        }
    }
} 