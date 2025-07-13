package com.samdoherty.aggregator.infrastructure.websocket.bitstamp.dto;

import lombok.Builder;

@Builder
public record SubscribeMessage(
        String channel
) {
}
