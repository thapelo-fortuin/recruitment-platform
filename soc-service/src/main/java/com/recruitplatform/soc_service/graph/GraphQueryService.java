package com.recruitplatform.soc_service.graph;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class GraphQueryService {

    private final Neo4jClient neo4jClient;

    public GraphQueryService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    // Query 1: Alerts affecting a given service
    public Collection<Map<String, Object>> alertsByService(String service) {
        return neo4jClient.query("""
                MATCH (a:Alert)-[:AFFECTS]->(s:Service {name: $service})
                RETURN a.alertId AS alertId, a.ruleName AS ruleName, a.severity AS severity,
                       a.status AS status, a.createdAt AS createdAt, a.description AS description
                ORDER BY a.createdAt DESC
                """)
                .bind(service).to("service")
                .fetch()
                .all();
    }

    // Query 2: Users linked to high-severity (HIGH or CRITICAL) alerts
    public Collection<Map<String, Object>> usersWithHighSeverityAlerts() {
        return neo4jClient.query("""
                MATCH (a:Alert)-[:AFFECTS_USER]->(u:User)
                WHERE a.severity IN ['HIGH', 'CRITICAL']
                RETURN u.id AS userId,
                       collect(DISTINCT a.ruleName) AS rules,
                       count(DISTINCT a) AS alertCount
                ORDER BY alertCount DESC
                """)
                .fetch()
                .all();
    }

    // Query 3: Controls (and the documented procedure) for a specific alert
    public Collection<Map<String, Object>> controlsForAlert(String alertId) {
        return neo4jClient.query("""
                MATCH (a:Alert {alertId: $alertId})-[:INDICATES]->(t:Threat)
                OPTIONAL MATCH (t)-[:MITIGATED_BY]->(c:Control)
                OPTIONAL MATCH (t)-[:DOCUMENTED_IN]->(d:Document)
                RETURN t.name AS threat, t.mitreId AS mitreId,
                       collect(DISTINCT c.name) AS controls,
                       collect(DISTINCT d.name) AS procedures
                """)
                .bind(alertId).to("alertId")
                .fetch()
                .all();
    }

    // Query 4: Dependency impact — which services depend on a given (possibly failing) service
    public Collection<Map<String, Object>> dependencyImpact(String service) {
        return neo4jClient.query("""
                MATCH (s:Service {name: $service})<-[:DEPENDS_ON]-(dependent:Service)
                RETURN dependent.name AS dependentService
                """)
                .bind(service).to("service")
                .fetch()
                .all();
    }

    // Query 5: Threats linked to alerts affecting a given service
    public Collection<Map<String, Object>> threatsForService(String service) {
        return neo4jClient.query("""
                MATCH (a:Alert)-[:AFFECTS]->(s:Service {name: $service})
                MATCH (a)-[:INDICATES]->(t:Threat)
                RETURN DISTINCT t.name AS threat, t.mitreId AS mitreId, t.description AS description
                """)
                .bind(service).to("service")
                .fetch()
                .all();
    }

    // Supporting query, useful in Phase F: full evidence trail for one alert
    public Collection<Map<String, Object>> evidenceForAlert(String alertId) {
        return neo4jClient.query("""
                MATCH (a:Alert {alertId: $alertId})
                OPTIONAL MATCH (e:Event)-[:CREATED_ALERT]->(a)
                OPTIONAL MATCH (e)<-[:TRIGGERED]-(u:User)
                OPTIONAL MATCH (a)-[:INDICATES]->(t:Threat)-[:MITIGATED_BY]->(c:Control)
                RETURN a.alertId AS alertId, a.ruleName AS ruleName, a.severity AS severity,
                       a.description AS description, a.recommendedAction AS recommendedAction,
                       collect(DISTINCT e.eventId) AS eventIds,
                       collect(DISTINCT u.id) AS users,
                       collect(DISTINCT t.name) AS threats,
                       collect(DISTINCT c.name) AS controls
                """)
                .bind(alertId).to("alertId")
                .fetch()
                .all();
    }
}