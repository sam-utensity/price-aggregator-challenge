package com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto;

import lombok.Builder;

@Builder
public record WebsocketMessage<M>(
        Event event,
        String channel,
        M data
) {
}
