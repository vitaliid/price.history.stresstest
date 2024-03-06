package com.smarttrader.price.history.service.stresstest.service;

import com.smarttrader.price.history.service.stresstest.dto.request.TestRequest;
import com.smarttrader.price.history.service.stresstest.dto.response.StatisticsResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class StatisticsWorker {

    @Getter
    private final AtomicInteger generallyRequestsAmount = new AtomicInteger();

    @Getter
    private final AtomicInteger generallySuccessfulResponseAmount = new AtomicInteger();

    @Getter
    private final AtomicInteger generallyErrorResponseAmount = new AtomicInteger();

    @Getter
    private final Map<String, String> errorsFromResponses = new ConcurrentHashMap<>();

    @Getter
    private final List<String> generalErrors = Collections.synchronizedList(new ArrayList<>());

    private TestRequest testRequest;
    private Instant startedAt;
    private Instant willEndAt;
    private Instant endedAt;

    public void testStartedWithConfiguration(TestRequest testRequest) {
        this.startedAt = Instant.now();
        this.willEndAt = startedAt.plusSeconds(testRequest.getDurationInMinutes() * 60L);
        this.testRequest = testRequest;
    }

    public StatisticsResponse getStatistics() {
        final StatisticsResponse statisticsResponse = new StatisticsResponse();

        final Instant now = Instant.now();

        if (testRequest != null) {
            statisticsResponse.setStartedAt(startedAt);
            statisticsResponse.setWillEndAt(willEndAt);
            statisticsResponse.setEndedAt(Optional.ofNullable(endedAt).map(Instant::toString).orElse("-"));
            statisticsResponse.setConnections(testRequest.getConnectionsNumber());
            statisticsResponse.setDuration(Duration.between(Optional.ofNullable(startedAt)
                                    .orElse(now),
                            Optional.ofNullable(endedAt).orElse(Instant.now())).toString()
                    .substring(2)
                    .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                    .toLowerCase());
        }

        statisticsResponse.setTotalRequests(generallyRequestsAmount.get());
        statisticsResponse.setTotalSuccessResponses(generallySuccessfulResponseAmount.get());
        statisticsResponse.setTotalFailedResponses(generallyErrorResponseAmount.get());
        statisticsResponse.setGeneralErrors(generalErrors.stream().toList());
        statisticsResponse.setErrorsFromResponses(errorsFromResponses);
        return statisticsResponse;
    }

    public void endOfTest() {
        if (endedAt == null) {
            endedAt = Instant.now();
        }
    }

    public void reset() {
        generallyRequestsAmount.set(0);
        generallySuccessfulResponseAmount.set(0);
        generallyErrorResponseAmount.set(0);

        errorsFromResponses.clear();
        generalErrors.clear();

        startedAt = null;
        endedAt = null;
        willEndAt = null;

        testRequest = null;
    }
}
