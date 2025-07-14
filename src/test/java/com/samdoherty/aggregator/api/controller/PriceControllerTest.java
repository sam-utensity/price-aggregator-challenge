package com.samdoherty.aggregator.api.controller;

import com.samdoherty.aggregator.domain.model.Instrument;
import com.samdoherty.aggregator.domain.model.Price;
import com.samdoherty.aggregator.domain.service.PriceAggregatorService;
import com.samdoherty.aggregator.infrastructure.websocket.bitstamp.BitstampWebsocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PriceController.class)
class PriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceAggregatorService priceAggregatorService;

    @MockitoBean
    private BitstampWebsocket bitStampWebsocket;

    @Test
    void testGetPrices_ValidSymbol_ReturnsPrice() throws Exception {
        Price price = Price.builder().price(new BigDecimal("100.43")).build();
        when(priceAggregatorService.getPrice(any(Instrument.class))).thenReturn(price);
        mockMvc.perform(get("/prices/BTC-USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(100.43));
    }

    @Test
    void testGetPrices_SymbolNotFound_ThrowsException() throws Exception {
        when(priceAggregatorService.getPrice(any(Instrument.class))).thenReturn(null);
        mockMvc.perform(get("/prices/ETH-USD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetPrices_BlankSymbol_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/prices/ "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetPrices_InvalidSymbolFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/prices/INVALID"))
                .andExpect(status().isBadRequest());
    }
}
