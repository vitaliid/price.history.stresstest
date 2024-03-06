package com.smarttrader.price.history.service.stresstest.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;
    private DistributionSummary distributionSummary;

    @PostConstruct
    private void createMetricEndpoint() {
        distributionSummary = DistributionSummary.builder("app_com_smarttrader_price_history_stresstest_latency")
                //.description("Latency for History service responses")
                .baseUnit("ms")
                //.tag("testTag", "testTagValue").
                .publishPercentileHistogram(true)
                .publishPercentiles(0.15,0.5,0.75,0.99)
                .register(meterRegistry);
    }

    public void addToMetrics(long latencyInMs) {
        distributionSummary.record(latencyInMs);
    }
}
