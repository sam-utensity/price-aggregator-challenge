package com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Event {
    SUBSCRIBE("bts:subscribe"),
    SUBSCRIBE_SUCCESS("bts:subscription_succeeded"),
    TRADE("trade"),
    RECONNECT_REQUEST("bts:request_reconnect"),
    HEARTBEAT("bts:heartbeat");

    @JsonValue
    private final String value;

    Event(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
