package com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto;

import lombok.Builder;

/**
 * Generic Bitstamp websocket message. They use a standardized format for send/receive
 */
@Builder
public record WebsocketMessage<M>(
        Event event,
        String channel,
        M data
) {
}
