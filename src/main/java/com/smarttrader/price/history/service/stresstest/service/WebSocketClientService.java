package com.smarttrader.price.history.service.stresstest.service;

import com.smarttrader.price.history.service.stresstest.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.resources.ConnectionProvider;

import java.net.URI;

@Slf4j
@Service
public class WebSocketClientService {

    private final ReactorNettyWebSocketClient client;
    private final HttpHeaders httpHeaders;
    private final AppConfig appConfig;

    public WebSocketClientService(AppConfig appConfig) {
        this.appConfig = appConfig;
        ConnectionProvider webFluxConnectionProvider = ConnectionProvider.create("webflux", 5000);
        HttpClient httpClient = HttpClient.create(webFluxConnectionProvider);

        this.client = new ReactorNettyWebSocketClient(httpClient,
                () -> WebsocketClientSpec.builder().maxFramePayloadLength(524288000));
        this.httpHeaders = new HttpHeaders();

        log.info("Target is {}", appConfig.getTarget());
    }

    public Mono<Void> execute(WebSocketHandler handler) {
        return client.execute(
                URI.create(appConfig.getTarget()),
                httpHeaders,
                handler);
    }
}
