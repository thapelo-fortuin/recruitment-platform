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
                       a.status AS status, a.createdAt AS createdAt, a.description AS description,
                       [{label: 'Alert', id: a.alertId}, {label: 'Service', id: s.name}] AS nodes,
                       [{source: a.alertId, type: 'AFFECTS', target: s.name}] AS relationships
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
                       count(DISTINCT a) AS alertCount,
                       collect(DISTINCT {label: 'User', id: u.id}) +
                       collect(DISTINCT {label: 'Alert', id: a.alertId}) AS nodes,
                       collect(DISTINCT {source: a.alertId, type: 'AFFECTS_USER', target: u.id}) AS relationships
                ORDER BY alertCount DESC
                """)
                .fetch()
                .all();
    }

    // Query 3: Controls (and the documented procedure) for a specific alert
    public Collection<Map<String, Object>> controlsForAlert(String alertId) {
        return neo4jClient.query("""
                MATCH (a:Alert {alertId: $alertId})-[:INDICATES]->(t:Threat)
                OPTIONAL MATCH (c:Control)-[:MITIGATES]->(t)
                OPTIONAL MATCH (d:Document)-[:SUPPORTS_RESPONSE_TO]->(t)
                OPTIONAL MATCH (t)-[:EXPLOITS]->(v:Vulnerability)
                RETURN t.name AS threat, t.mitreId AS mitreId,
                       collect(DISTINCT c.name) AS controls,
                       collect(DISTINCT CASE WHEN d IS NULL THEN null ELSE {name: d.name, content: d.content} END) AS procedures,
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {cweId: v.cweId, name: v.name, sourceUrl: v.sourceUrl} END) AS vulnerabilities,
                       collect(DISTINCT {label: 'Alert', id: a.alertId}) +
                       collect(DISTINCT {label: 'Threat', id: t.name}) +
                       collect(DISTINCT CASE WHEN c IS NULL THEN null ELSE {label: 'Control', id: c.name} END) +
                       collect(DISTINCT CASE WHEN d IS NULL THEN null ELSE {label: 'Document', id: d.name, content: d.content} END) +
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {label: 'Vulnerability', id: v.cweId, name: v.name, sourceUrl: v.sourceUrl} END) AS nodes,
                       collect(DISTINCT {source: a.alertId, type: 'INDICATES', target: t.name}) +
                       collect(DISTINCT CASE WHEN c IS NULL THEN null ELSE {source: c.name, type: 'MITIGATES', target: t.name} END) +
                       collect(DISTINCT CASE WHEN d IS NULL THEN null ELSE {source: d.name, type: 'SUPPORTS_RESPONSE_TO', target: t.name} END) +
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {source: t.name, type: 'EXPLOITS', target: v.cweId} END) AS relationships
                """)
                .bind(alertId).to("alertId")
                .fetch()
                .all();
    }

    // Query 4: Dependency impact — which services depend on a given (possibly failing) service
    public Collection<Map<String, Object>> dependencyImpact(String service) {
        return neo4jClient.query("""
                MATCH path=(s:Service {name: $service})<-[:DEPENDS_ON*1..]-(dependent:Service)
                RETURN DISTINCT dependent.name AS dependentService,
                       [node IN nodes(path) | {label: 'Service', id: node.name}] AS nodes,
                       [rel IN relationships(path) | {source: startNode(rel).name, type: type(rel), target: endNode(rel).name}] AS relationships
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
                OPTIONAL MATCH (t)-[:EXPLOITS]->(v:Vulnerability)
                RETURN t.name AS threat, t.mitreId AS mitreId, t.description AS description,
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {cweId: v.cweId, name: v.name, sourceUrl: v.sourceUrl} END) AS vulnerabilities,
                       collect(DISTINCT {label: 'Alert', id: a.alertId}) +
                       collect(DISTINCT {label: 'Service', id: s.name}) +
                       collect(DISTINCT {label: 'Threat', id: t.name}) +
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {label: 'Vulnerability', id: v.cweId, name: v.name, sourceUrl: v.sourceUrl} END) AS nodes,
                       collect(DISTINCT {source: a.alertId, type: 'AFFECTS', target: s.name}) +
                       collect(DISTINCT {source: a.alertId, type: 'INDICATES', target: t.name}) +
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {source: t.name, type: 'EXPLOITS', target: v.cweId} END) AS relationships
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
                OPTIONAL MATCH (u)-[:HAS_ROLE]->(r:Role)
                OPTIONAL MATCH (e)-[:GENERATED_BY]->(s:Service)
                OPTIONAL MATCH (e)-[:TARGETED]->(ep:Endpoint)
                OPTIONAL MATCH (a)-[:AFFECTS]->(affectedService:Service)
                OPTIONAL MATCH (a)-[:AFFECTS_ENDPOINT]->(affectedEndpoint:Endpoint)
                OPTIONAL MATCH (a)-[:INDICATES]->(t:Threat)
                OPTIONAL MATCH (c:Control)-[:MITIGATES]->(t)
                OPTIONAL MATCH (d:Document)-[:SUPPORTS_RESPONSE_TO]->(t)
                OPTIONAL MATCH (t)-[:EXPLOITS]->(v:Vulnerability)
                OPTIONAL MATCH (i:Incident)-[:CONTAINS]->(a)
                RETURN a.alertId AS alertId, a.ruleName AS ruleName, a.severity AS severity,
                       a.description AS description, a.recommendedAction AS recommendedAction,
                       collect(DISTINCT e.eventId) AS eventIds,
                       collect(DISTINCT u.id) AS users,
                       collect(DISTINCT r.name) AS roles,
                       collect(DISTINCT t.name) AS threats,
                       collect(DISTINCT c.name) AS controls,
                       collect(DISTINCT CASE WHEN d IS NULL THEN null ELSE {name: d.name, content: d.content} END) AS procedures,
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {cweId: v.cweId, name: v.name, sourceUrl: v.sourceUrl} END) AS vulnerabilities,
                       collect(DISTINCT i.incidentId) AS incidents,
                       collect(DISTINCT {label: 'Alert', id: a.alertId}) +
                       collect(DISTINCT CASE WHEN e IS NULL THEN null ELSE {label: 'Event', id: e.eventId} END) +
                       collect(DISTINCT CASE WHEN u IS NULL THEN null ELSE {label: 'User', id: u.id} END) +
                       collect(DISTINCT CASE WHEN r IS NULL THEN null ELSE {label: 'Role', id: r.name} END) +
                       collect(DISTINCT CASE WHEN s IS NULL THEN null ELSE {label: 'Service', id: s.name} END) +
                       collect(DISTINCT CASE WHEN ep IS NULL THEN null ELSE {label: 'Endpoint', id: ep.path} END) +
                       collect(DISTINCT CASE WHEN affectedService IS NULL THEN null ELSE {label: 'Service', id: affectedService.name} END) +
                       collect(DISTINCT CASE WHEN affectedEndpoint IS NULL THEN null ELSE {label: 'Endpoint', id: affectedEndpoint.path} END) +
                       collect(DISTINCT CASE WHEN t IS NULL THEN null ELSE {label: 'Threat', id: t.name} END) +
                       collect(DISTINCT CASE WHEN c IS NULL THEN null ELSE {label: 'Control', id: c.name} END) +
                       collect(DISTINCT CASE WHEN d IS NULL THEN null ELSE {label: 'Document', id: d.name, content: d.content} END) +
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {label: 'Vulnerability', id: v.cweId, name: v.name, sourceUrl: v.sourceUrl} END) +
                       collect(DISTINCT CASE WHEN i IS NULL THEN null ELSE {label: 'Incident', id: i.incidentId} END) AS nodes,
                       collect(DISTINCT CASE WHEN e IS NULL THEN null ELSE {source: e.eventId, type: 'CREATED_ALERT', target: a.alertId} END) +
                       collect(DISTINCT CASE WHEN u IS NULL OR e IS NULL THEN null ELSE {source: u.id, type: 'TRIGGERED', target: e.eventId} END) +
                       collect(DISTINCT CASE WHEN u IS NULL OR r IS NULL THEN null ELSE {source: u.id, type: 'HAS_ROLE', target: r.name} END) +
                       collect(DISTINCT CASE WHEN e IS NULL OR s IS NULL THEN null ELSE {source: e.eventId, type: 'GENERATED_BY', target: s.name} END) +
                       collect(DISTINCT CASE WHEN e IS NULL OR ep IS NULL THEN null ELSE {source: e.eventId, type: 'TARGETED', target: ep.path} END) +
                       collect(DISTINCT CASE WHEN affectedService IS NULL THEN null ELSE {source: a.alertId, type: 'AFFECTS', target: affectedService.name} END) +
                       collect(DISTINCT CASE WHEN affectedEndpoint IS NULL THEN null ELSE {source: a.alertId, type: 'AFFECTS_ENDPOINT', target: affectedEndpoint.path} END) +
                       collect(DISTINCT CASE WHEN t IS NULL THEN null ELSE {source: a.alertId, type: 'INDICATES', target: t.name} END) +
                       collect(DISTINCT CASE WHEN c IS NULL THEN null ELSE {source: c.name, type: 'MITIGATES', target: t.name} END) +
                       collect(DISTINCT CASE WHEN d IS NULL THEN null ELSE {source: d.name, type: 'SUPPORTS_RESPONSE_TO', target: t.name} END) +
                       collect(DISTINCT CASE WHEN v IS NULL THEN null ELSE {source: t.name, type: 'EXPLOITS', target: v.cweId} END) +
                       collect(DISTINCT CASE WHEN i IS NULL THEN null ELSE {source: i.incidentId, type: 'CONTAINS', target: a.alertId} END) AS relationships
                """)
                .bind(alertId).to("alertId")
                .fetch()
                .all();
    }
}
