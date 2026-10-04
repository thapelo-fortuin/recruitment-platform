package com.recruitplatform.soc_service.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
@Component
public class GraphSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GraphSeeder.class);

    private final Neo4jClient neo4jClient;

    public GraphSeeder(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public void run(String... args) {
        log.info("Seeding security knowledge graph...");

        // Threats, referencing real MITRE ATT&CK technique IDs where applicable
        merge("Threat", "name", "Credential Attack",
                "mitreId", "T1110", "description", "Brute Force: repeated attempts to guess valid credentials");
        merge("Threat", "name", "Broken Access Control",
                "mitreId", "T1078", "description", "Valid Accounts: use of stolen or unauthorised credentials/permissions to access resources");
        merge("Threat", "name", "Denial of Service",
                "mitreId", "T1499", "description", "Endpoint Denial of Service: flooding a service with requests to degrade availability");
        merge("Threat", "name", "Service Disruption",
                "mitreId", "T1499.004", "description", "Application Exhaustion Flood: causing repeated application-level errors/failures");

        // Controls
        merge("Control", "name", "Account Lockout", "description", "Temporarily lock an account after repeated failed logins");
        merge("Control", "name", "Multi-Factor Authentication", "description", "Require a second factor after suspicious login activity");
        merge("Control", "name", "Rate Limiting", "description", "Restrict the number of requests a client can make in a time window");
        merge("Control", "name", "Input Validation", "description", "Reject malformed or out-of-range requests before they reach business logic");
        merge("Control", "name", "Health Monitoring", "description", "Track service health and restart or alert on repeated failures");

        // Procedures / documents
        merge("Document", "name", "Credential Attack Response Runbook",
                "content", "1. Lock the account. 2. Notify the user. 3. Require MFA re-enrolment. 4. Review source IPs for further abuse.");
        merge("Document", "name", "DoS Response Runbook",
                "content", "1. Identify offending IP(s). 2. Apply rate limiting or a temporary block. 3. Scale or restart affected service if needed.");

        // Mitigations: which controls address which threats
        relate("Threat", "name", "Credential Attack", "MITIGATED_BY", "Control", "name", "Account Lockout");
        relate("Threat", "name", "Credential Attack", "MITIGATED_BY", "Control", "name", "Multi-Factor Authentication");
        relate("Threat", "name", "Broken Access Control", "MITIGATED_BY", "Control", "name", "Rate Limiting");
        relate("Threat", "name", "Denial of Service", "MITIGATED_BY", "Control", "name", "Rate Limiting");
        relate("Threat", "name", "Service Disruption", "MITIGATED_BY", "Control", "name", "Health Monitoring");
        relate("Threat", "name", "Service Disruption", "MITIGATED_BY", "Control", "name", "Input Validation");

        // Procedures document how to respond to each threat
        relate("Threat", "name", "Credential Attack", "DOCUMENTED_IN", "Document", "name", "Credential Attack Response Runbook");
        relate("Threat", "name", "Denial of Service", "DOCUMENTED_IN", "Document", "name", "DoS Response Runbook");

        // Assets: your five services, as things worth protecting
        for (String service : new String[]{"api-gateway", "auth-service", "job-service", "application-service", "soc-service"}) {
            merge("Asset", "name", service, "type", "microservice");
            relate("Service", "name", service, "IS_A", "Asset", "name", service);
            // Dependency relationships, mirroring real inter-service calls and compose depends_on
            relate("Service", "name", "api-gateway", "DEPENDS_ON", "Service", "name", "auth-service");
            relate("Service", "name", "api-gateway", "DEPENDS_ON", "Service", "name", "job-service");
            relate("Service", "name", "api-gateway", "DEPENDS_ON", "Service", "name", "application-service");
            relate("Service", "name", "application-service", "DEPENDS_ON", "Service", "name", "auth-service");
            relate("Service", "name", "application-service", "DEPENDS_ON", "Service", "name", "job-service");
        }

        // Roles from your app
        merge("Role", "name", "CANDIDATE");
        merge("Role", "name", "RECRUITER");

        log.info("Graph seeding complete.");
    }

    private void merge(String label, String keyProp, String keyValue, String... otherProps) {
        StringBuilder set = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("key", keyValue);

        for (int i = 0; i < otherProps.length; i += 2) {
            set.append(", n.`").append(otherProps[i]).append("` = $p").append(i);
            params.put("p" + i, otherProps[i + 1]);
        }

        neo4jClient.query("MERGE (n:" + label + " {" + keyProp + ": $key}) SET n.updatedAt = timestamp()" + set)
                .bindAll(params)
                .run();
    }

    private void relate(String fromLabel, String fromKey, String fromValue,
                        String relType, String toLabel, String toKey, String toValue) {
        neo4jClient.query(
                        "MERGE (a:" + fromLabel + " {" + fromKey + ": $from}) " +
                                "MERGE (b:" + toLabel + " {" + toKey + ": $to}) " +
                                "MERGE (a)-[:" + relType + "]->(b)")
                .bind(fromValue).to("from")
                .bind(toValue).to("to")
                .run();
    }
}