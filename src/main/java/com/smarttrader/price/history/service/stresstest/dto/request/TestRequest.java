package com.smarttrader.price.history.service.stresstest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestRequest {
    @Schema(description = "Number of connections. Should be a positive number")
    private int connectionsNumber;
    @Schema(description = "Duration of test in minutes. Should be a positive number")
    private int durationInMinutes;
    @Schema(description = "Number of requests in a batch. Should be a positive number or 0")
    private int batchSize;
    @Schema(description = "Max number of requests in a minute. Should be a positive number")
    private int maxInMinute;
    @Schema(description = "Candles number. Should be a positive number. For all tests it is 4000")
    private int candlesNumber;
    @Schema(description = """
            Global time. Format is "PnYnMnDTnHnMnS".
            Examples: 2 years = "P2Y", 1 hour = "PT1H"
            """)
    private String globalTime;
    @Schema(description = """
            Array of periods. Example [
                "1m",
                "5m"
              ]""")
    private List<String> periodList = new ArrayList<>();
    @Schema(description = """
            Array of symbols. Example [
                "AUD/USD"
              ]""")
    private List<String> symbols = new ArrayList<>();
}
