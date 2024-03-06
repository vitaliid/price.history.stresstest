package com.smarttrader.price.history.service.stresstest.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodOfCandle {
    PERIOD_1m(1),
    PERIOD_5m(2),
    PERIOD_10m(3),
    PERIOD_15m(4),
    PERIOD_30m(5),
    PERIOD_1h(6),
    PERIOD_2h(7),
    PERIOD_3h(8),
    PERIOD_4h(9),
    PERIOD_8h(10),
    PERIOD_1d(11),
    PERIOD_1w(12),
    PERIOD_1M(13),
    PERIOD_1y(14);

    private final int id;

    public static PeriodOfCandle parseConfigValue(String value) {
        return PeriodOfCandle.valueOf("PERIOD_" + value);
    }
}
