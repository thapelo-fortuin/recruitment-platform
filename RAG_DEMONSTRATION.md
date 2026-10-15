# Security graph RAG demonstration

Use an administrator account and open **Admin dashboard → Security insights**. The interface displays the generated answer, the Cypher retrieval method, and expandable evidence including graph nodes and relationships. This is template-based GraphRAG: the question analyzer selects a known query pattern, Neo4j retrieves matching graph evidence, and a response template summarizes the returned records. It does not call an LLM or use vector embeddings.

## Demonstration 1: explain an alert

**Question:** “Why was the latest alert created and what should be done?”

**Preparation:** Generate an alert by submitting at least five failed logins for the same email within five minutes, or use an existing alert.

**Retrieval:** `evidenceForAlert` traverses alert → related events, event ← user, event → service/endpoint, alert → threat, control → threat, document → threat, threat → vulnerability, and incident → alert.

**Evidence shown:** Retrieved `Alert`, `Event`, `User` (where known), `Service`, `Endpoint`, `Threat`, `Control`, `Document`, `Vulnerability`, and `Incident` nodes, plus relationship triples such as `CREATED_ALERT`, `INDICATES`, `MITIGATES`, and `SUPPORTS_RESPONSE_TO`.

**Generated answer:** The template explains the rule and severity, describes the alert, lists the related threat and weaknesses, names mitigations and response procedures, and includes the recommended action. Exact IDs and counts depend on the running event data.

**Method:** Cypher retrieval followed by template-based answer generation.

## Demonstration 2: find mitigations and response procedures

**Question:** “What controls mitigate the latest alert?”

**Retrieval:** Resolve the latest alert, then follow `Alert-[:INDICATES]->Threat`, `(Control)-[:MITIGATES]->Threat`, and `(Document)-[:SUPPORTS_RESPONSE_TO]->Threat`.

**Evidence shown:** Threat name and MITRE technique, control names, vulnerability references and source URLs, and response procedure names with their stored runbook content.

**Generated answer:** The template returns the threat, available controls, and documented procedure. If the alert has no threat link, the response says that no threat/control information is linked.

**Method:** Cypher pattern selection and graph traversal; no vector search or external LLM.

## Demonstration 3: estimate dependency impact

**Question:** “What is the dependency impact if application-service fails?”

**Retrieval:** Traverse one or more `DEPENDS_ON` relationships in reverse from `application-service` to dependent services.

**Evidence shown:** Each returned dependent service and the retrieval method.

**Generated answer:** The template lists services that depend directly or transitively on `application-service`, or reports that none are currently modeled.

**Method:** Cypher variable-length graph traversal and template-based answer generation.

## Traceability demonstration

For a failed-login alert, the expected flow is: auth service emits `LOGIN_FAILED` → SOC stores the event → the rule detects five events for one user in five minutes → alert is stored → event nodes link to the alert in Neo4j → alert indicates `Credential Attack` → controls and the response procedure are retrieved → the UI displays answer, method, evidence nodes, and relationship triples. The exact live values should be captured from the running UI for the final assessment submission.
