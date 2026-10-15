package com.recruitplatform.soc_service.graph;

import java.util.Map;

public class GraphSeedData {

    // ruleName -> threat name, used both for seeding and for linking alerts to threats
    public static final Map<String, String> RULE_TO_THREAT = Map.of(
            "MULTIPLE_FAILED_LOGINS", "Credential Attack",
            "REPEATED_UNAUTHORISED_ACCESS", "Broken Access Control",
            "ABNORMAL_REQUEST_RATE", "Denial of Service",
            "REPEATED_SERVICE_ERRORS", "Service Disruption",
            "SERVICE_HEALTH_FAILURE", "Service Disruption"
    );
}
