package com.smarttrader.price.history.service.stresstest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttrader.price.history.service.stresstest.config.SubscriptionConfig;
import com.smarttrader.price.history.service.stresstest.dto.request.TestRequest;
import com.smarttrader.price.history.service.stresstest.dto.request.client.HeartbeatRequest;
import com.smarttrader.price.history.service.stresstest.dto.request.client.SubscriptionRequest;
import com.smarttrader.price.history.service.stresstest.dto.response.client.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestWorker {

    private static final int FLUX_FLAT_MAP_CONCURRENCY = 50000;

    private final WebSocketClientService webSocketClientService;
    private final StatisticsWorker statisticsWorker;
    private final RequestStatisticsWorker requestStatisticsWorker;


    private final SubscriptionConfig subscriptionsConfig;

    private final SubscriptionRequestGenerator subscriptionRequestGenerator;

    private final ObjectMapper objectMapper;

    private boolean subscriptionStarted = false;

    public void subscribe(TestRequest testConfiguration) {
        if (subscriptionStarted) {
            log.info("There was a try to run test again.");
            return;
        }

        final Instant endTimeOfTest = Instant.now().plusSeconds(testConfiguration.getDurationInMinutes() * 30L);
        log.info("Test will end at {}", endTimeOfTest);

        try {
            subscriptionRequestGenerator.getTestData(testConfiguration);
        } catch (Exception e) {
            statisticsWorker.getGeneralErrors().add(e.toString());
            throw e;
        }

        statisticsWorker.testStartedWithConfiguration(testConfiguration);
        final int timeoutBetweenBatches = subscriptionRequestGenerator.getTimeoutBetweenBatchesInSeconds();

        Flux.fromStream(IntStream.range(0, testConfiguration.getConnectionsNumber()).boxed())
                .window(subscriptionsConfig.getThrottlingConnections())
                .zipWith(Flux.interval(Duration.ZERO, Duration.ofSeconds(subscriptionsConfig.getThrottlingSeconds())))
                .flatMap(Tuple2::getT1, FLUX_FLAT_MAP_CONCURRENCY)
                .flatMap(sessionNumber -> {
                            log.info("Opening session: {}/{}", sessionNumber + 1, testConfiguration.getConnectionsNumber());
                            return webSocketClientService
                                    .execute(session ->
                                            createSender(session, testConfiguration, timeoutBetweenBatches)
                                                    //END OF TEST is assured by this function
                                                    .take(Duration.ofMinutes(testConfiguration.getDurationInMinutes()))
                                                    .and(createReceiver(session))
                                    )
                                    //END OF TEST is assured by this function
                                    // .take(Duration.ofMinutes(testConfiguration.getDurationInMinutes()))
                                    ;

                        }, FLUX_FLAT_MAP_CONCURRENCY
                )
                .subscribe();

        subscriptionStarted = true;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("summary", statisticsWorker.getStatistics());
        statistics.put("response time, ms", requestStatisticsWorker.getResponseTime());
        return statistics;
    }

    public Map<String, Object> getRequests() {
        return requestStatisticsWorker.getRequests().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().getLatency()));
    }


    public Map<String, Object> getStatisticsPerMin() {
        return requestStatisticsWorker.getStatisticsPerMin();
    }


    public void reset() {
        requestStatisticsWorker.reset();
        statisticsWorker.reset();
        subscriptionStarted = false;
    }

    private Mono<Void> createSender(WebSocketSession session, TestRequest testConfiguration, int timeoutBetweenBatches) {
        return session.send(
                        createDataSender(session, testConfiguration, timeoutBetweenBatches)
                                .doOnNext(webSocketMessage ->
                                        statisticsWorker.getGenerallyRequestsAmount().getAndIncrement()
                                )
                                .mergeWith(createHeartbeatSender(session))
                               /* .doOnNext(webSocketMessage ->
                                        log.info("---> {} Sending message: {}", webSocketMessage.getType(), webSocketMessage.getPayloadAsText())
                                )*/
                )
                .doOnError(throwable -> {
                    log.error("[After request] Error happened with session {}", session.getId(), throwable);
                    statisticsWorker.getGeneralErrors().add(throwable.toString());
                })
                .doOnCancel(() -> log.warn("[After request] Session {} unexpectedly cancelled", session.getId()));
    }

    private Flux<WebSocketMessage> createDataSender(WebSocketSession session, TestRequest testConfiguration, int timeoutBetweenRequests) {
        return Flux.generate((SynchronousSink<SubscriptionRequest> subscriptionRequestFluxSink) ->
                        subscriptionRequestFluxSink.next(subscriptionRequestGenerator.generate())
                )

                //having max in minute
                .window(testConfiguration.getMaxInMinute())
                .zipWith(Flux.interval(Duration.ZERO, Duration.ofMinutes(1)))
                .flatMap(Tuple2::getT1, FLUX_FLAT_MAP_CONCURRENCY)

                //having batch size
                .window(testConfiguration.getBatchSize())
                .zipWith(Flux.interval(Duration.ZERO, Duration.ofSeconds(timeoutBetweenRequests)))
                .flatMap(Tuple2::getT1, FLUX_FLAT_MAP_CONCURRENCY)

                .handle((sameRequest, sink) -> {
                    try {
                        sink.next(session.textMessage(objectMapper.writeValueAsString(sameRequest)));
                        requestStatisticsWorker.checkRequest(sameRequest.getId());
                    } catch (JsonProcessingException e) {
                        log.error("[During sending] Error happened with request {}",
                                sameRequest, e);
                        sink.error(e);
                    }
                });
    }

    private Flux<WebSocketMessage> createHeartbeatSender(WebSocketSession session) {
        return Mono.just(HeartbeatRequest.JSON_VALUE_AS_BYTES)
                .map(heartbeatRequest ->
                        session.pingMessage(dataBufferFactory -> dataBufferFactory.wrap(heartbeatRequest))
                )
                .repeat()
                .delayElements(Duration.ofSeconds(5))
                .subscribeOn(Schedulers.immediate())
                .publishOn(Schedulers.immediate());
    }

    private Flux<WebSocketMessage> createReceiver(WebSocketSession session) {
        return session.receive()
              /*  .doOnNext(webSocketMessage ->
                        log.info("<--- {} [{}] Received message: {}", webSocketMessage.getType(), session.getId(), webSocketMessage.getPayloadAsText())
                )*/
                .log(log.getName(), Level.ALL)
                //handle text messages only
                .filter(webSocketMessage -> webSocketMessage.getType() == WebSocketMessage.Type.TEXT)
                .handle((BiConsumer<WebSocketMessage, SynchronousSink<WebSocketMessage>>) (webSocketMessage, synchronousSink) -> {
                    try {
                        SubscriptionResponse subscriptionResponse = objectMapper.readValue(webSocketMessage.getPayloadAsText(), SubscriptionResponse.class);
                        requestStatisticsWorker.checkResponse(subscriptionResponse.getId());

                        if (StringUtils.isBlank(subscriptionResponse.getError()) &&
                                !CollectionUtils.isEmpty(subscriptionResponse.getHistory())) {
                            statisticsWorker.getGenerallySuccessfulResponseAmount().getAndIncrement();
                        } else {
                            statisticsWorker.getGenerallyErrorResponseAmount().getAndIncrement();
                            if (StringUtils.isNotBlank(subscriptionResponse.getError())) {
                                statisticsWorker.getErrorsFromResponses().put(subscriptionResponse.getId(), subscriptionResponse.getError());
                            }
                        }
                        synchronousSink.next(webSocketMessage);
                    } catch (JsonProcessingException throwable) {
                        log.error("Error happened while parsing message {}", webSocketMessage.getPayloadAsText(), throwable);
                        synchronousSink.error(throwable);
                    }
                })
                .doOnError(throwable -> {
                    log.error("[During receiving] Error happened with session {}", session.getId(), throwable);
                    statisticsWorker.getGeneralErrors().add(throwable.toString());
                })
                .doOnCancel(() -> {
                    log.warn("[During receiving] Session {} unexpectedly cancelled", session.getId());
                    statisticsWorker.endOfTest();
                })
                .doOnComplete(() -> log.warn("[During receiving] Session {} unexpectedly completed", session.getId()));
    }
}
