package com.smarttrader.price.history.service.stresstest.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class StatisticsResponse {
    private Instant startedAt;
    private Instant willEndAt;
    private String endedAt;
    private String duration;
    private long connections = 0;

    private int totalRequests;
    private int totalSuccessResponses;
    private int totalFailedResponses;

    private List<String> generalErrors;
    private Map<String, String> errorsFromResponses;
}
