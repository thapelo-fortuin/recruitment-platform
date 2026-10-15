# SOC detection rules

The SOC detection engine evaluates persisted events every 15 seconds. A 10-minute cooldown prevents repeated alert creation for the same rule and affected entity.

| Rule | Purpose and input | Detection condition | Severity | Alert and suggested response |
|---|---|---|---|---|
| `MULTIPLE_FAILED_LOGINS` | Detect credential guessing. Input: `LOGIN_FAILED` from `auth-service`. | At least 5 failures for the same user identifier during the last 5 minutes. | HIGH | Alert links the user, auth service, login endpoint, and related event IDs. Temporarily lock the account and require MFA. |
| `REPEATED_UNAUTHORISED_ACCESS` | Detect protected-resource probing. Input: `UNAUTHORISED_ACCESS` from gateway/auth/job/application; login endpoint excluded to avoid counting a failed login twice. | At least 8 events for the same non-login endpoint during 5 minutes. | MEDIUM; HIGH when events come from at least 3 distinct source IPs. | Alert identifies service, endpoint, source IP, and related events. Rate-limit or temporarily block offending IPs. |
| `ABNORMAL_REQUEST_RATE` | Detect possible denial of service. Input: `API_REQUEST` from `api-gateway`. | At least 100 requests from one source IP in 1 minute. | HIGH | Alert links the gateway and up to 20 representative event IDs. Throttle or temporarily block the IP. |
| `REPEATED_SERVICE_ERRORS` | Detect repeated server failures. Input: `SERVICE_ERROR` from gateway/auth/job/application. | At least 5 5xx events from one service in 5 minutes. | HIGH | Alert links the service and related events. Check logs and health; consider restarting the service. |
| `SERVICE_HEALTH_FAILURE` | Detect unavailable application services. Input: transition events from `soc-health-monitor`. | A service health probe fails; the monitor emits one event per state transition, so one failure event raises an alert. | HIGH | Alert links the affected service and health event. Inspect service health, logs, and dependencies; restore availability. |

Each alert stores an ID, timestamp, rule name, severity, status, description, related event IDs, affected service/user/endpoint when known, and recommended action. The alert is synchronized to Neo4j and linked to its events, threat, and incident.
