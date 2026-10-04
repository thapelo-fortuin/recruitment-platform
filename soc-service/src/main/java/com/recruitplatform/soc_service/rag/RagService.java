package com.recruitplatform.soc_service.rag;

import com.recruitplatform.soc_service.graph.GraphQueryService;
import com.recruitplatform.soc_service.model.Alert;
import com.recruitplatform.soc_service.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RagService {

    private final QuestionAnalyzer analyzer;
    private final GraphQueryService graphQueryService;
    private final AlertRepository alertRepository;

    public RagService(QuestionAnalyzer analyzer, GraphQueryService graphQueryService,
                      AlertRepository alertRepository) {
        this.analyzer = analyzer;
        this.graphQueryService = graphQueryService;
        this.alertRepository = alertRepository;
    }

    public RagResponse ask(String question) {
        RagIntent intent = analyzer.analyse(question);

        return switch (intent.type()) {
            case ALERTS_BY_SERVICE -> {
                String service = intent.service().orElse(null);
                if (service == null) yield unresolvable(question, "No known service was mentioned in the question.");
                var evidence = graphQueryService.alertsByService(service);
                yield new RagResponse(question, AnswerTemplates.alertsByService(service, evidence),
                        "Cypher: alerts AFFECTS service, filtered by service name", evidence);
            }
            case HIGH_SEVERITY_USERS -> {
                var evidence = graphQueryService.usersWithHighSeverityAlerts();
                yield new RagResponse(question, AnswerTemplates.highSeverityUsers(evidence),
                        "Cypher: users AFFECTS_USER alerts, filtered by severity HIGH/CRITICAL", evidence);
            }
            case CONTROLS_FOR_ALERT -> {
                String alertId = resolveAlertId(intent);
                if (alertId == null) yield unresolvable(question, "No alert ID was given and no alert exists yet.");
                var evidence = graphQueryService.controlsForAlert(alertId);
                yield new RagResponse(question, AnswerTemplates.controlsForAlert(evidence),
                        "Cypher: alert INDICATES threat MITIGATED_BY control, DOCUMENTED_IN procedure", evidence);
            }
            case DEPENDENCY_IMPACT -> {
                String service = intent.service().orElse(null);
                if (service == null) yield unresolvable(question, "No known service was mentioned in the question.");
                var evidence = graphQueryService.dependencyImpact(service);
                yield new RagResponse(question, AnswerTemplates.dependencyImpact(service, evidence),
                        "Cypher: service <-DEPENDS_ON- dependent services", evidence);
            }
            case THREATS_FOR_SERVICE -> {
                String service = intent.service().orElse(null);
                if (service == null) yield unresolvable(question, "No known service was mentioned in the question.");
                var evidence = graphQueryService.threatsForService(service);
                yield new RagResponse(question, AnswerTemplates.threatsForService(service, evidence),
                        "Cypher: alerts AFFECTS service, INDICATES threat", evidence);
            }
            case ALERT_EXPLANATION -> {
                String alertId = resolveAlertId(intent);
                if (alertId == null) yield unresolvable(question, "No alert ID was given and no alert exists yet.");
                var evidence = graphQueryService.evidenceForAlert(alertId);
                yield new RagResponse(question, AnswerTemplates.alertExplanation(evidence),
                        "Cypher: full evidence trail — event TRIGGERED by user, CREATED_ALERT, INDICATES threat, MITIGATED_BY control",
                        evidence);
            }
            case UNKNOWN -> unresolvable(question,
                    "Could not match this question to a known pattern. Try asking about alerts, threats, controls, dependencies, or high-severity users.");
        };
    }

    // if no alertId is named in the question, fall back to the most recent alert
    // (optionally narrowed by a mentioned service), since that's almost always what a demo question means
    private String resolveAlertId(RagIntent intent) {
        if (intent.alertId().isPresent()) {
            return intent.alertId().get();
        }
        List<Alert> all = alertRepository.findAllByOrderByCreatedAtDesc();
        if (intent.service().isPresent()) {
            return all.stream()
                    .filter(a -> intent.service().get().equals(a.getAffectedService()))
                    .findFirst()
                    .map(Alert::getAlertId)
                    .orElse(all.isEmpty() ? null : all.get(0).getAlertId());
        }
        return all.isEmpty() ? null : all.get(0).getAlertId();
    }

    private RagResponse unresolvable(String question, String reason) {
        return new RagResponse(question, reason, "No matching Cypher pattern was found", List.of());
    }
}