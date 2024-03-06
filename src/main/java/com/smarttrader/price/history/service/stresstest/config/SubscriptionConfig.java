package com.smarttrader.price.history.service.stresstest.config;


import com.smarttrader.price.history.service.stresstest.dto.PeriodOfCandle;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "subscription")
@Getter
@Setter
public class SubscriptionConfig {

    @Value("${subscription.throttling.connections}")
    private Integer throttlingConnections;

    @Value("${subscription.throttling.seconds}")
    private Integer throttlingSeconds;

    @Value("${subscription.connections}")
    private Integer connectionsNumber;

    @Value("${subscription.durationInMinutes}")
    private Integer durationInMinutes;


    //THROTTLING
    @Value("${subscription.batch-size}")
    private Integer batchSize;

    @Value("${subscription.max-in-minute}")
    private Integer maxInMinute;


    //REQUEST PARAMS
    @Value("${subscription.request.global-time}")
    private Period globalTime;

    @Value("${subscription.request.candles}")
    private String candlesNumber;

    @Value("${subscription.request.periods}")
    private List<String> periods;

    public List<PeriodOfCandle> getPeriods() {
        return periods.stream()
                .map(PeriodOfCandle::parseConfigValue)
                .toList();
    }
}
