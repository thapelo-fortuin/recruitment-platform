package com.recruitplatform.soc_service.detection;

import com.recruitplatform.soc_service.model.Alert;
import com.recruitplatform.soc_service.model.Event;
import com.recruitplatform.soc_service.repository.AlertRepository;
import com.recruitplatform.soc_service.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);

    // requests to the login endpoint are excluded from the 401/403 rule below,
    // because auth-service already reports the same failure as LOGIN_FAILED —
    // counting both would double-count one bad password.
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final int COOLDOWN_MINUTES = 10;

    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;

    public DetectionEngine(EventRepository eventRepository, AlertRepository alertRepository) {
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
    }

    @Scheduled(fixedDelay = 15000) // every 15 seconds
    public void runRules() {
        detectBruteForceLogin();
        detectRepeatedUnauthorised();
        detectRequestSpike();
        detectServiceFailure();
    }

    // Rule 1: Multiple failed logins
    // Purpose: catch credential-guessing (brute force / credential stuffing) against one account.
    // Input events: LOGIN_FAILED
    // Logic: 5 or more LOGIN_FAILED events for the same userId within 5 minutes.
    // Severity: HIGH. Response: lock the account or require MFA.
    private void detectBruteForceLogin() {
        LocalDateTime window = LocalDateTime.now().minusMinutes(5);
        List<Event> failures = eventRepository.findByServiceNameAndEventTypeAndTimestampAfter(
                "auth-service", "LOGIN_FAILED", window);

        Map<String, List<Event>> byUser = failures.stream()
                .filter(e -> e.getUserId() != null)
                .collect(Collectors.groupingBy(Event::getUserId));

        for (Map.Entry<String, List<Event>> entry : byUser.entrySet()) {
            String userId = entry.getKey();
            List<Event> userFailures = entry.getValue();
            if (userFailures.size() < 5) continue;
            if (alertRepository.existsByRuleNameAndAffectedUserAndCreatedAtAfter(
                    "MULTIPLE_FAILED_LOGINS", userId, LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES))) {
                continue;
            }

            Alert alert = newAlert("MULTIPLE_FAILED_LOGINS", "HIGH",
                    userFailures.size() + " failed login attempts for " + userId + " in the last 5 minutes",
                    "Lock the account temporarily and require MFA on next successful login");
            alert.setAffectedService("auth-service");
            alert.setAffectedUser(userId);
            alert.setAffectedEndpoint(LOGIN_ENDPOINT);
            alert.setAffectedIp(userFailures.get(userFailures.size() - 1).getSourceIp());
            alert.setRelatedEvents(idsOf(userFailures));
            alertRepository.save(alert);
            log.warn("ALERT raised: {} for user {}", alert.getRuleName(), userId);
        }
    }

    // Rule 2: Repeated unauthorised access
    // Purpose: catch an attacker probing one endpoint without valid credentials or permissions.
    // Input events: UNAUTHORISED_ACCESS (401/403), excluding the login endpoint (see constant above).
    // Logic: 8 or more such events on the same endpoint within 5 minutes.
    // Severity: MEDIUM, or HIGH if it also spans 3+ different source IPs. Response: rate-limit or block the endpoint.
    private void detectRepeatedUnauthorised() {
        LocalDateTime window = LocalDateTime.now().minusMinutes(5);

        // the finder above needs a concrete endpoint, so instead we scan recent events grouped by endpoint;
        // simplest correct approach: reuse findByServiceNameAndEventTypeAndTimestampAfter per known service
        for (String service : List.of("api-gateway", "job-service", "application-service", "auth-service")) {
            List<Event> serviceEvents = eventRepository.findByServiceNameAndEventTypeAndTimestampAfter(
                    service, "UNAUTHORISED_ACCESS", window);

            Map<String, List<Event>> byEndpoint = serviceEvents.stream()
                    .filter(e -> e.getEndpoint() != null && !LOGIN_ENDPOINT.equals(e.getEndpoint()))
                    .collect(Collectors.groupingBy(Event::getEndpoint));

            for (Map.Entry<String, List<Event>> entry : byEndpoint.entrySet()) {
                String endpoint = entry.getKey();
                List<Event> hits = entry.getValue();
                if (hits.size() < 8) continue;
                if (alertRepository.existsByRuleNameAndAffectedEndpointAndCreatedAtAfter(
                        "REPEATED_UNAUTHORISED_ACCESS", endpoint, LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES))) {
                    continue;
                }

                long distinctIps = hits.stream().map(Event::getSourceIp).distinct().count();
                String severity = distinctIps >= 3 ? "HIGH" : "MEDIUM";

                Alert alert = newAlert("REPEATED_UNAUTHORISED_ACCESS", severity,
                        hits.size() + " unauthorised requests to " + endpoint + " in the last 5 minutes from "
                                + distinctIps + " distinct IP(s)",
                        "Rate-limit or temporarily block the endpoint for the offending IP(s)");
                alert.setAffectedService(service);
                alert.setAffectedEndpoint(endpoint);
                alert.setAffectedIp(hits.get(hits.size() - 1).getSourceIp());
                alert.setRelatedEvents(idsOf(hits));
                alertRepository.save(alert);
                log.warn("ALERT raised: {} for endpoint {}", alert.getRuleName(), endpoint);
            }
        }
    }

    // Rule 3: Abnormal request rate (possible DoS)
    // Purpose: catch one client or service flooding the platform with requests.
    // Input events: API_REQUEST from api-gateway.
    // Logic: more than 100 requests from the same source IP within 1 minute.
    // Severity: HIGH. Response: throttle or temporarily block the source IP at the gateway.
    private void detectRequestSpike() {
        LocalDateTime window = LocalDateTime.now().minusMinutes(1);
        List<Event> events = eventRepository.findByServiceNameAndEventTypeAndTimestampAfter(
                "api-gateway", "API_REQUEST", window);

        Map<String, List<Event>> byIp = events.stream()
                .filter(e -> e.getSourceIp() != null)
                .collect(Collectors.groupingBy(Event::getSourceIp));

        for (Map.Entry<String, List<Event>> entry : byIp.entrySet()) {
            String ip = entry.getKey();
            List<Event> hits = entry.getValue();
            if (hits.size() < 100) continue;
            if (alertRepository.existsByRuleNameAndAffectedServiceAndCreatedAtAfter(
                    "ABNORMAL_REQUEST_RATE", "api-gateway", LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES))) {
                continue;
            }{
                // see note below the code block
            }

            Alert alert = newAlert("ABNORMAL_REQUEST_RATE", "HIGH",
                    hits.size() + " requests from " + ip + " in the last minute",
                    "Throttle or temporarily block this source IP at the gateway");
            alert.setAffectedService("api-gateway");
            alert.setAffectedIp(ip);
            alert.setRelatedEvents(idsOf(hits.subList(0, Math.min(hits.size(), 20)))); // cap stored IDs
            alertRepository.save(alert);
            log.warn("ALERT raised: {} for IP {}", alert.getRuleName(), ip);
        }
    }

    // Rule 4: Repeated service errors
    // Purpose: catch a service that's failing, whether from a bug, overload, or an attack exploiting it.
    // Input events: SERVICE_ERROR (5xx).
    // Logic: 5 or more SERVICE_ERROR events from the same service within 5 minutes.
    // Severity: HIGH. Response: check the service's health and logs, consider restarting it.
    private void detectServiceFailure() {
        LocalDateTime window = LocalDateTime.now().minusMinutes(5);
        for (String service : List.of("api-gateway", "auth-service", "job-service", "application-service")) {
            List<Event> errors = eventRepository.findByServiceNameAndEventTypeAndTimestampAfter(
                    service, "SERVICE_ERROR", window);
            if (errors.size() < 5) continue;
            if (alertRepository.existsByRuleNameAndAffectedServiceAndCreatedAtAfter(
                    "REPEATED_SERVICE_ERRORS", service, LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES))) {
                continue;
            }

            Alert alert = newAlert("REPEATED_SERVICE_ERRORS", "HIGH",
                    errors.size() + " server errors (5xx) from " + service + " in the last 5 minutes",
                    "Check " + service + "'s logs and health endpoint; consider restarting it");
            alert.setAffectedService(service);
            alert.setRelatedEvents(idsOf(errors));
            alertRepository.save(alert);
            log.warn("ALERT raised: {} for service {}", alert.getRuleName(), service);
        }
    }

    private Alert newAlert(String ruleName, String severity, String description, String recommendedAction) {
        Alert alert = new Alert();
        alert.setAlertId("alt-" + UUID.randomUUID());
        alert.setRuleName(ruleName);
        alert.setSeverity(severity);
        alert.setStatus("OPEN");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setDescription(description);
        alert.setRecommendedAction(recommendedAction);
        return alert;
    }

    private String idsOf(List<Event> events) {
        return events.stream().map(Event::getEventId).collect(Collectors.joining(","));
    }
}