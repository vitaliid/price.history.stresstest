package com.smarttrader.price.history.service.stresstest.dto.request.client;

import lombok.Getter;

@Getter
public class HeartbeatRequest {
    public static final byte[] JSON_VALUE_AS_BYTES = "{\"heartbeat\":\"ping\"}".getBytes();

    private final String heartbeat = "ping";
}
