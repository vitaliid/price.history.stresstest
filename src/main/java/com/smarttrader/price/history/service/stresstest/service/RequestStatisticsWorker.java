package com.smarttrader.price.history.service.stresstest.service;

import com.smarttrader.price.history.service.stresstest.metrics.BusinessMetricsService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestStatisticsWorker {

    @Getter
    private final Map<String, RequestInfo> requests = new ConcurrentHashMap<>();

    private final BusinessMetricsService businessMetricsService;


    static class RequestInfo {
        private final Instant requestTime;

        @Getter
        private Instant responseTime;

        @Getter
        private long latency = -1;

        public RequestInfo() {
            requestTime = Instant.now();
        }

        public long response() {
            responseTime = Instant.now();
            latency = Duration.between(requestTime, responseTime).toMillis();
            return latency;
        }

        public boolean responded() {
            return responseTime != null;
        }
    }

    public void checkRequest(String requestId) {
        requests.put(requestId, new RequestInfo());
    }

    public void checkResponse(String requestId) {
        long latency = requests.computeIfAbsent(requestId, key -> new RequestInfo()).response();
        businessMetricsService.addToMetrics(latency);
    }

    public Map<String, Object> getResponseTime() {
        LongSummaryStatistics responded = requests.values().stream()
                .filter(RequestInfo::responded)
                .mapToLong(RequestInfo::getLatency)
                .summaryStatistics();

        List<Map.Entry<String, RequestInfo>> notResponded = requests.entrySet().stream()
                .filter(requestInfo -> !requestInfo.getValue().responded())
                .toList();

        return requests.isEmpty() ? Collections.emptyMap()
                : Map.of(
                "responded", responded,
                "not responded", notResponded.size());
    }

    public void reset() {
        requests.clear();
    }

    public Map<String, Object> getStatisticsPerMin() {
        return getRequests().values().stream()
                .filter(RequestInfo::responded)
                .collect(Collectors.groupingBy(requestInfo -> {
                    LocalDateTime localDateTime = LocalDateTime.ofInstant(requestInfo.getResponseTime(), ZoneId.systemDefault());
                    return localDateTime.getHour() + ":" + localDateTime.getMinute();
                }))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().stream()
                        .mapToLong(RequestInfo::getLatency)
                        .summaryStatistics()));
    }
}
