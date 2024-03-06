package com.smarttrader.price.history.service.stresstest.controller;


import com.smarttrader.price.history.service.stresstest.dto.request.TestRequest;
import com.smarttrader.price.history.service.stresstest.service.TestWorker;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class TestController {

    private final TestWorker testWorker;

    @PostMapping(path = "start", consumes = {"application/json"})
    public void start(@RequestBody(content = @Content(examples = {
            @ExampleObject(
                    name = "Test sample",
                    summary = "Test example",
                    value = """
                            {
                              "connectionsNumber": 100,
                              "durationInMinutes": 10,
                              "batchSize": 5,
                              "maxInMinute": 30,
                              "candlesNumber": 4000,
                              "globalTime": "P2Y",
                              "periodList": [
                                "1m",
                                "5m"
                              ],
                              "symbols": [
                                "AUD/USD"
                              ]
                            }""")}))
                      @org.springframework.web.bind.annotation.RequestBody TestRequest testRequest) {
        testWorker.subscribe(testRequest);
    }

    @PostMapping("reset")
    public void start() {
        testWorker.reset();
    }

    @GetMapping("statistics")
    public Map<String, Object> statistics() {
        return testWorker.getStatistics();
    }

    @Hidden
    @GetMapping("requests")
    public Map<String, Object> requests() {
        return testWorker.getRequests();
    }

    @Hidden
    @GetMapping("statistics/min")
    public Map<String, Object> getStatisticsPerMin() {
        return testWorker.getStatisticsPerMin();
    }
}
