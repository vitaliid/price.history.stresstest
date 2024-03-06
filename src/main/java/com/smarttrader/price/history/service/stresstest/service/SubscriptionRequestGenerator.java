package com.smarttrader.price.history.service.stresstest.service;

import com.smarttrader.price.history.service.stresstest.config.InstrumentConfig;
import com.smarttrader.price.history.service.stresstest.dto.PeriodOfCandle;
import com.smarttrader.price.history.service.stresstest.dto.request.TestRequest;
import com.smarttrader.price.history.service.stresstest.dto.request.client.SubscriptionRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRequestGenerator {

    private final static String APPLICATION_ID = "test_app";

    private final Random randomGenerator = new Random();

    private final InstrumentConfig instrumentConfig;

    private int candlesNumber;
    private long globalTimeInSeconds;
    private long nowInSeconds;
    private List<Integer> periodIds;
    private List<String> symbols;

    @Getter
    private int timeoutBetweenBatchesInSeconds;

    public void getTestData(TestRequest testRequest) {

        Validate.isTrue(testRequest.getConnectionsNumber() > 0,
                "connectionsNumber should be a positive number");
        Validate.isTrue(testRequest.getDurationInMinutes() > 0,
                "durationInMinutes should be a positive number");
        Validate.isTrue(testRequest.getMaxInMinute() > 0,
                "maxInMinute should be a positive number");
        Validate.isTrue(testRequest.getCandlesNumber() > 0,
                "candlesNumber should be a positive number");

        if (!CollectionUtils.isEmpty(symbols)) {
            Validate.isTrue(new HashSet<>(instrumentConfig.getInstruments(false)).containsAll(symbols),
                    "Not supported values in 'symbols' field");
        }

        if (testRequest.getBatchSize() < 1) {
            testRequest.setBatchSize(testRequest.getMaxInMinute());
        }

        candlesNumber = testRequest.getCandlesNumber();
        symbols = CollectionUtils.isEmpty(testRequest.getSymbols())
                ? instrumentConfig.getInstruments(false)
                : testRequest.getSymbols();
        periodIds = testRequest.getPeriodList().stream()
                .map(PeriodOfCandle::parseConfigValue)
                .map(PeriodOfCandle::getId)
                .toList();

        final ZonedDateTime now = ZonedDateTime.now();
        //Period globalPeriod = Period.parse(testRequest.getGlobalTime());
        //globalTimeInSeconds = now.minus(globalPeriod).toEpochSecond();
        nowInSeconds = now.toEpochSecond();

        try {
            Duration duration = DatatypeFactory.newInstance().newDuration(testRequest.getGlobalTime());
            long timeInMillis = duration.getTimeInMillis(Calendar.getInstance());
            this.globalTimeInSeconds = now.minus(timeInMillis, ChronoUnit.MILLIS).toEpochSecond();
            log.info("Global time {}", this.globalTimeInSeconds);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException("Global time is wrong", e);
        }

        //counting timeout
        int numberOfBatches = testRequest.getMaxInMinute() / testRequest.getBatchSize();
        if (testRequest.getMaxInMinute() % testRequest.getBatchSize() > 0) {
            numberOfBatches++;
        }
        timeoutBetweenBatchesInSeconds = 60 / numberOfBatches;
        log.info("Timeout between batches will be {} seconds", timeoutBetweenBatchesInSeconds);
    }

    public SubscriptionRequest generate() {
        return new SubscriptionRequest(APPLICATION_ID, getRandomSymbol(), getRandomPeriodId(), candlesNumber, getRandomEndTime());
    }

    private int getRandomPeriodId() {
        int randomIndex = randomGenerator.nextInt(periodIds.size());
        return periodIds.get(randomIndex);
    }

    private String getRandomSymbol() {
        int randomIndex = randomGenerator.nextInt(symbols.size());
        return symbols.get(randomIndex);
    }

    private long getRandomEndTime() {
        return randomGenerator.nextLong(globalTimeInSeconds, nowInSeconds);
    }
}
