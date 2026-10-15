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
                "mitreId", "T1110", "description", "Brute Force: repeated attempts to guess valid credentials",
                "sourceUrl", "https://attack.mitre.org/techniques/T1110/");
        merge("Threat", "name", "Broken Access Control",
                "mitreId", "T1078", "description", "Valid Accounts: use of stolen or unauthorised credentials/permissions to access resources",
                "sourceUrl", "https://attack.mitre.org/techniques/T1078/");
        merge("Threat", "name", "Denial of Service",
                "mitreId", "T1499", "description", "Endpoint Denial of Service: flooding a service with requests to degrade availability",
                "sourceUrl", "https://attack.mitre.org/techniques/T1499/");
        merge("Threat", "name", "Service Disruption",
                "mitreId", "T1499.004", "description", "Application Exhaustion Flood: causing repeated application-level errors/failures",
                "sourceUrl", "https://attack.mitre.org/techniques/T1499/004/");

        // Security weaknesses are explicitly represented so threat-to-vulnerability paths are queryable.
        merge("Vulnerability", "cweId", "CWE-521", "name", "Weak Password Requirements",
                "description", "Weak password requirements can make accounts easier to compromise.",
                "sourceUrl", "https://cwe.mitre.org/data/definitions/521.html");
        merge("Vulnerability", "cweId", "CWE-284", "name", "Improper Access Control",
                "description", "Access-control weaknesses can allow unauthorized access to protected resources.",
                "sourceUrl", "https://cwe.mitre.org/data/definitions/284.html");
        merge("Vulnerability", "cweId", "CWE-400", "name", "Uncontrolled Resource Consumption",
                "description", "Uncontrolled resource consumption can reduce service availability.",
                "sourceUrl", "https://cwe.mitre.org/data/definitions/400.html");

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
        relate("Control", "name", "Account Lockout", "MITIGATES", "Threat", "name", "Credential Attack");
        relate("Control", "name", "Multi-Factor Authentication", "MITIGATES", "Threat", "name", "Credential Attack");
        relate("Control", "name", "Rate Limiting", "MITIGATES", "Threat", "name", "Broken Access Control");
        relate("Control", "name", "Rate Limiting", "MITIGATES", "Threat", "name", "Denial of Service");
        relate("Control", "name", "Health Monitoring", "MITIGATES", "Threat", "name", "Service Disruption");
        relate("Control", "name", "Input Validation", "MITIGATES", "Threat", "name", "Service Disruption");

        // Procedures document how to respond to each threat
        relate("Document", "name", "Credential Attack Response Runbook", "SUPPORTS_RESPONSE_TO", "Threat", "name", "Credential Attack");
        relate("Document", "name", "DoS Response Runbook", "SUPPORTS_RESPONSE_TO", "Threat", "name", "Denial of Service");
        relate("Document", "name", "Credential Attack Response Runbook", "DESCRIBES", "Control", "name", "Account Lockout");
        relate("Document", "name", "Credential Attack Response Runbook", "DESCRIBES", "Control", "name", "Multi-Factor Authentication");
        relate("Document", "name", "DoS Response Runbook", "DESCRIBES", "Control", "name", "Rate Limiting");

        relate("Threat", "name", "Credential Attack", "EXPLOITS", "Vulnerability", "cweId", "CWE-521");
        relate("Threat", "name", "Broken Access Control", "EXPLOITS", "Vulnerability", "cweId", "CWE-284");
        relate("Threat", "name", "Denial of Service", "EXPLOITS", "Vulnerability", "cweId", "CWE-400");
        relate("Threat", "name", "Service Disruption", "EXPLOITS", "Vulnerability", "cweId", "CWE-400");

        relate("Threat", "name", "Credential Attack", "TARGETS", "Service", "name", "auth-service");
        relate("Threat", "name", "Broken Access Control", "TARGETS", "Service", "name", "auth-service");
        relate("Threat", "name", "Broken Access Control", "TARGETS", "Service", "name", "application-service");
        relate("Threat", "name", "Denial of Service", "TARGETS", "Service", "name", "api-gateway");
        relate("Threat", "name", "Denial of Service", "TARGETS", "Service", "name", "job-service");
        relate("Threat", "name", "Service Disruption", "TARGETS", "Service", "name", "application-service");

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

        expose("api-gateway", "/api/v1/auth/login");
        expose("api-gateway", "/api/v1/jobs");
        expose("api-gateway", "/api/v1/applications");
        expose("auth-service", "/api/v1/auth/login");
        expose("auth-service", "/api/v1/auth/register");
        expose("job-service", "/api/v1/jobs");
        expose("application-service", "/api/v1/applications");
        expose("soc-service", "/api/v1/rag/ask");

        // Roles from your app
        merge("Role", "name", "CANDIDATE");
        merge("Role", "name", "RECRUITER");
        merge("Role", "name", "ADMIN");

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

    private void expose(String service, String endpoint) {
        merge("Endpoint", "path", endpoint, "name", endpoint);
        relate("Service", "name", service, "EXPOSES", "Endpoint", "path", endpoint);
    }
}
