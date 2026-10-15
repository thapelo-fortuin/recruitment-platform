# SOC knowledge graph model

The SOC service uses Neo4j for application structure, security reference data, and runtime event/alert evidence. `GraphSeeder` inserts the application and reference data at startup. `GraphSyncService` adds runtime events and alerts. An incident is created when an alert is linked, grouping alerts by service, threat, and calendar day.

## Node labels and key properties

| Label | Key | Important properties | Origin |
|---|---|---|---|
| `User` | `id` | user identifier (currently email for login events) | Auth events and alerts |
| `Service` | `name` | service name | seeded application structure and runtime events |
| `Endpoint` | `path` | API path | seeded service routes and runtime events |
| `Asset` | `name` | `type` | seeded service inventory |
| `Event` | `eventId` | `eventType`, `severity`, `timestamp`, `statusCode`, `httpMethod`, `sourceIp`, `message`, `correlationId`, `affectedEntity`, `userRole` when known | SOC event ingestion and health monitor |
| `Alert` | `alertId` | `ruleName`, `severity`, `status`, `createdAt`, `description`, `recommendedAction` | Detection engine |
| `Incident` | `incidentId` | `title`, `status`, `createdAt`, `updatedAt` | Created and grouped when alerts are synchronized |
| `Threat` | `name` | `mitreId`, `description`, `sourceUrl` | Security reference seed data |
| `Control` | `name` | `description` | Security reference seed data |
| `Vulnerability` | `cweId` | `name`, `description`, `sourceUrl` | CWE weakness reference seed data; this is reference data, not a claim that the app was scanned and found vulnerable |
| `Role` | `name` | application role | Seed data (`CANDIDATE`, `RECRUITER`, `ADMIN`) |
| `Document` | `name` | `content` | Seeded response procedures |

## Relationships

| From | Relationship | To | Purpose |
|---|---|---|---|
| `User` | `TRIGGERED` | `Event` | Attribute login events to the submitted user identifier |
| `Event` | `GENERATED_BY` | `Service` | Identify the event-producing service |
| `Event` | `TARGETED` | `Endpoint` | Identify the endpoint involved in an event |
| `Event` | `CREATED_ALERT` | `Alert` | Preserve event-to-alert traceability |
| `Alert` | `AFFECTS` | `Service` | Identify the affected service |
| `Alert` | `AFFECTS_USER` | `User` | Identify an affected user where known |
| `Alert` | `AFFECTS_ENDPOINT` | `Endpoint` | Identify an affected endpoint where known |
| `Alert` | `INDICATES` | `Threat` | Map detection rules to security threats |
| `Threat` | `TARGETS` | `Service` | Model the likely service exposure |
| `Threat` | `EXPLOITS` | `Vulnerability` | Connect threats to recognized weakness categories |
| `Control` | `MITIGATES` | `Threat` | Retrieve applicable controls |
| `Incident` | `CONTAINS` | `Alert` | Group same-day alerts by affected service and threat |
| `Service` | `DEPENDS_ON` | `Service` | Query direct and transitive dependency impact |
| `Service` | `EXPOSES` | `Endpoint` | Model application routes |
| `User` | `HAS_ROLE` | `Role` | Successful auth login events include the known account role |
| `Document` | `DESCRIBES` | `Control` | Link response documentation to controls |
| `Document` | `SUPPORTS_RESPONSE_TO` | `Threat` | Retrieve a response procedure for a threat |

Runtime event nodes and alert nodes are synced into Neo4j when they are stored in SOC Postgres. The graph queries cover alerts by service, high-severity users, controls/procedures for an alert, dependency impact, threats for a service, and a full alert evidence trail. The evidence trail returns node summaries and relationship triples for display in Security insights.

## Data provenance

1. Application structure: services, dependencies, roles, and endpoints seeded from the application/Compose design.
2. Phase 2 runtime: events collected from gateway/auth/job/application services and service health checks; alerts emitted by detection rules.
3. Security reference: MITRE ATT&CK technique identifiers and CWE weakness categories, with source links on seeded records.

The CWE nodes describe reference weaknesses associated with modeled threats. They do not assert that a vulnerability scanner found those weaknesses in this application.
