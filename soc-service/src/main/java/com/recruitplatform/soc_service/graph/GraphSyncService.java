package com.recruitplatform.soc_service.graph;

import com.recruitplatform.soc_service.model.Alert;
import com.recruitplatform.soc_service.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    private final Neo4jClient neo4jClient;

    public GraphSyncService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void syncEvent(Event event) {
        try {
            neo4jClient.query("""
                MERGE (e:Event {eventId: $eventId})
                SET e.eventType = $eventType, e.severity = $severity, e.timestamp = $timestamp,
                    e.statusCode = $statusCode, e.correlationId = $correlationId
                MERGE (s:Service {name: $serviceName})
                MERGE (e)-[:OCCURRED_IN]->(s)
                WITH e
                CALL (e) {
                    WITH e
                    WHERE $endpoint IS NOT NULL
                    MERGE (ep:Endpoint {path: $endpoint})
                    MERGE (e)-[:AT]->(ep)
                }
                CALL (e) {
                    WITH e
                    WHERE $userId IS NOT NULL
                    MERGE (u:User {id: $userId})
                    MERGE (u)-[:TRIGGERED]->(e)
                }
                """)
                    .bind(event.getEventId()).to("eventId")
                    .bind(event.getEventType()).to("eventType")
                    .bind(event.getSeverity()).to("severity")
                    .bind(event.getTimestamp().toString()).to("timestamp")
                    .bind(event.getStatusCode()).to("statusCode")
                    .bind(event.getCorrelationId()).to("correlationId")
                    .bind(event.getServiceName()).to("serviceName")
                    .bind(event.getEndpoint()).to("endpoint")
                    .bind(event.getUserId()).to("userId")
                    .run();
        } catch (Exception ex) {
            // graph sync must never break event ingestion, same fire-and-forget principle as Phase B
            log.warn("Could not sync event {} to Neo4j: {}", event.getEventId(), ex.getMessage());
        }
    }

    public void syncAlert(Alert alert) {
        try {
            neo4jClient.query("""
                MERGE (a:Alert {alertId: $alertId})
                SET a.ruleName = $ruleName, a.severity = $severity, a.status = $status,
                    a.description = $description, a.recommendedAction = $recommendedAction,
                    a.createdAt = $createdAt
                WITH a
                CALL (a) {
                    WITH a
                    WHERE $affectedService IS NOT NULL
                    MERGE (s:Service {name: $affectedService})
                    MERGE (a)-[:AFFECTS]->(s)
                }
                CALL (a) {
                    WITH a
                    WHERE $affectedUser IS NOT NULL
                    MERGE (u:User {id: $affectedUser})
                    MERGE (a)-[:AFFECTS_USER]->(u)
                }
                """)
                    .bind(alert.getAlertId()).to("alertId")
                    .bind(alert.getRuleName()).to("ruleName")
                    .bind(alert.getSeverity()).to("severity")
                    .bind(alert.getStatus()).to("status")
                    .bind(alert.getDescription()).to("description")
                    .bind(alert.getRecommendedAction()).to("recommendedAction")
                    .bind(alert.getCreatedAt().toString()).to("createdAt")
                    .bind(alert.getAffectedService()).to("affectedService")
                    .bind(alert.getAffectedUser()).to("affectedUser")
                    .run();

            // link this alert's related events, and to the threat its rule represents
            for (String eventId : splitIds(alert.getRelatedEvents())) {
                neo4jClient.query(
                                "MATCH (e:Event {eventId: $eventId}) " +
                                        "MATCH (a:Alert {alertId: $alertId}) " +
                                        "MERGE (e)-[:CREATED_ALERT]->(a)")
                        .bind(eventId).to("eventId")
                        .bind(alert.getAlertId()).to("alertId")
                        .run();
            }

            String threatName = GraphSeedData.RULE_TO_THREAT.get(alert.getRuleName());
            if (threatName != null) {
                neo4jClient.query(
                                "MATCH (a:Alert {alertId: $alertId}) " +
                                        "MATCH (t:Threat {name: $threatName}) " +
                                        "MERGE (a)-[:INDICATES]->(t)")
                        .bind(alert.getAlertId()).to("alertId")
                        .bind(threatName).to("threatName")
                        .run();
            }
        } catch (Exception ex) {
            log.warn("Could not sync alert {} to Neo4j: {}", alert.getAlertId(), ex.getMessage());
        }
    }

    private String[] splitIds(String relatedEvents) {
        if (relatedEvents == null || relatedEvents.isBlank()) return new String[0];
        return Arrays.stream(relatedEvents.split(",")).map(String::trim).toArray(String[]::new);
    }
}