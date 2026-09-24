package com.recruitplatform.auth_service.soc;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class SocEventClient {

    private static final Logger log = LoggerFactory.getLogger(SocEventClient.class);

    private final RestClient restClient;

    private final ExecutorService executor = new ThreadPoolExecutor(
            1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500),
            new ThreadPoolExecutor.DiscardPolicy());

    public SocEventClient(@Value("${soc.url:http://localhost:8084}") String socUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().baseUrl(socUrl).requestFactory(factory).build();
    }

    public void send(SocEvent event) {
        executor.execute(() -> {
            try {
                restClient.post()
                        .uri("/api/v1/events")
                        .body(event)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception ex) {
                log.warn("Could not send event to soc-service: {}", ex.getMessage());
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}