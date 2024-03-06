package com.smarttrader.price.history.service.stresstest.dto.response.client;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionResponse {
    private List<Object> header = new ArrayList<>();
    private List<Object> history = new ArrayList<>();
    private String error;

    public String getId() {
        return getHeader().get(6).toString();
    }
}
