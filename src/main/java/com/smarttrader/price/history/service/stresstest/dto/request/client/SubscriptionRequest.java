package com.smarttrader.price.history.service.stresstest.dto.request.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class SubscriptionRequest {

    private final String applicationId;
    private final List<Chart> charts = new ArrayList<>();

    public SubscriptionRequest(String applicationId, String symbol, int periodId, int count, long endTime) {
        this.applicationId = applicationId;
        this.charts.add(new Chart(symbol, periodId, count, endTime));
    }

    @Getter
    private static class Chart {
        private final static String ID_DELIMITER = "-";

        private final String id;
        private final String exchange = "FXCM";
        private final String type = "FOREX";
        private final String symbol;
        private final int periodId;
        private final int count;
        private final long endTime;

        private Chart(String symbol, int periodId, int count, long endTime) {
            this.symbol = symbol;
            this.periodId = periodId;
            this.count = count;
            this.endTime = endTime;
            this.id = periodId + ID_DELIMITER +
                    count + ID_DELIMITER +
                    symbol + ID_DELIMITER +
                    endTime + UUID.randomUUID();
        }
    }

    @JsonIgnore
    public String getId() {
        return charts.get(0).getId();
    }
}
