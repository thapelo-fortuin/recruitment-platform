package com.recruitplatform.soc_service.monitoring;

import com.recruitplatform.soc_service.graph.GraphSyncService;
import com.recruitplatform.soc_service.model.Event;
import com.recruitplatform.soc_service.repository.EventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceHealthMonitor {

    private final EventRepository eventRepository;
    private final GraphSyncService graphSyncService;
    private final RestClient restClient;
    private final Map<String, String> serviceHealthUrls = new LinkedHashMap<>();
    private final Map<String, Boolean> lastKnownState = new ConcurrentHashMap<>();

    public ServiceHealthMonitor(EventRepository eventRepository, GraphSyncService graphSyncService) {
        this.eventRepository = eventRepository;
        this.graphSyncService = graphSyncService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        serviceHealthUrls.put("api-gateway", "http://api-gateway:8080/health");
        serviceHealthUrls.put("auth-service", "http://auth-service:8081/health");
        serviceHealthUrls.put("job-service", "http://job-service:8082/health");
        serviceHealthUrls.put("application-service", "http://application-service:8083/health");
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void checkServices() {
        serviceHealthUrls.forEach(this::checkService);
    }

    private void checkService(String serviceName, String healthUrl) {
        boolean healthy;
        try {
            Map<?, ?> response = restClient.get().uri(healthUrl).retrieve().body(Map.class);
            healthy = response != null && "UP".equalsIgnoreCase(String.valueOf(response.get("status")));
        } catch (Exception exception) {
            healthy = false;
        }

        Boolean previous = lastKnownState.put(serviceName, healthy);
        if (previous != null && previous == healthy) return;
        recordTransition(serviceName, healthy);
    }

    private void recordTransition(String serviceName, boolean healthy) {
        Event event = new Event();
        event.setEventId("evt-" + UUID.randomUUID());
        event.setTimestamp(LocalDateTime.now());
        event.setServiceName("soc-health-monitor");
        event.setEventType(healthy ? "SERVICE_HEALTHY" : "SERVICE_HEALTH_FAILURE");
        event.setSeverity(healthy ? "INFO" : "HIGH");
        event.setEndpoint("/health");
        event.setHttpMethod("GET");
        event.setStatusCode(healthy ? 200 : 503);
        event.setMessage(serviceName + (healthy ? " health check passed" : " health check failed"));
        event.setCorrelationId("corr-" + UUID.randomUUID());
        event.setAffectedEntity(serviceName);
        Event saved = eventRepository.save(event);
        graphSyncService.syncEvent(saved);
    }
}
