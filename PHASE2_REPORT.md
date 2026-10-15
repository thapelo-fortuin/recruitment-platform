# Phase 2 implementation report

## Architecture and data flow

The API Gateway audits requests and assigns a correlation ID. Auth records login success/failure events; the job and application services send domain events and report failed requests. SOC stores the event record in PostgreSQL, periodically evaluates detection rules, and synchronizes event and alert evidence into Neo4j. The health monitor probes the gateway, auth, job, and application `/health` endpoints every 30 seconds and records health transitions. Administrators use the protected dashboard to review alerts and ask Security insights questions.

```text
Gateway / Auth / Job / Application / Health monitor
                       │ SOC event POST
                       ▼
              SOC event store (Postgres)
                 │                 │
        detection rules       graph sync
                 │                 ▼
                 └── alerts → Neo4j graph
                                   │ Cypher retrieval
                                   ▼
                       template-based RAG answer
                          answer + evidence
```

## Requirement coverage

| Specification requirement | Implementation evidence |
|---|---|
| Events from gateway, authentication, two domain services | Gateway request audit, auth login events, job/application domain and error events are submitted to SOC. |
| Health information | SOC health monitor records `SERVICE_HEALTHY`/`SERVICE_HEALTH_FAILURE` on service state transitions. |
| Event fields | SOC `Event` contains event ID, timestamp, service, type, severity, user, IP, endpoint, method, status, message, correlation ID, and affected entity. |
| At least 3 detection rules | Five rules are documented in `DETECTION_RULES.md`; thresholds, severity, alert contents, and responses are stated there. |
| Alert handling | SOC stores alerts, the administrator dashboard displays them, and graph sync links alert, events, threat, and incident. |
| Required graph concepts and relations | See `KNOWLEDGE_GRAPH_MODEL.md`; includes Incident and Vulnerability nodes and the specified security/application relationships. |
| At least 5 graph queries | Six query endpoints cover alerts by service, high-severity users, controls/procedures, dependency impact, threats by service, and alert evidence. |
| Grounded natural-language RAG with evidence | Security insights selects a query pattern, retrieves Neo4j records, generates template answers, and shows retrieval method, nodes, and relationships. |
| Three RAG demonstrations | See `RAG_DEMONSTRATION.md` for questions, retrieval patterns, evidence types, generated answer behavior, and method. |
| Trace from event to RAG answer | The failed-login workflow is described in `RAG_DEMONSTRATION.md`. Live screenshots/outputs should be captured from the running instance. |

## Design choices and limitations

- Template-based GraphRAG was selected because it is deterministic and explainable for a small assessment dataset. It supports known intent patterns; arbitrary natural-language questions may be reported as unsupported.
- The health monitor checks whether each service responds to `/health`. These endpoints are liveness checks and do not currently verify database readiness or expose Docker's container state.
- Health events are written on state transitions to avoid producing a health event every 30 seconds. A first successful probe establishes state and is recorded as healthy.
- Incidents group alerts by service, mapped threat, and calendar day. This is a simple correlation model, not a full incident-management lifecycle.
- CWE records are reference weakness categories linked to modeled threats; they are not evidence of a confirmed vulnerability in the application.
- Gateway audit cannot identify an authenticated user in every request, so user attribution is populated only where a service knows the user, such as auth login events.
- Event clients submit asynchronously and use a bounded queue. When SOC is unavailable or the queue is full, events may be delayed or dropped; this prototype is not a durable message-broker pipeline.

## Rebuild and demonstrate

From the Compose project root, run `docker compose up -d --build`. Confirm the SOC service is healthy, sign in as an administrator, and open **Security insights**. Generate the failed-login scenario and run the three questions in `RAG_DEMONSTRATION.md`. Capture the UI output and evidence for the final submission.
