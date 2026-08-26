# Java Camel Fanout Server Requirements

## 1. Purpose

The system shall provide a Java-based fanout proxy server that receives HTTP requests, evaluates database-configured routing/filtering rules, and forwards accepted requests to one or more downstream targets.

The server shall run in OpenShift and use Apache Camel as the integration and routing framework.

## 2. Scope

The fanout server shall:

- Accept inbound HTTP requests.
- Match each request against an ordered set of rules.
- Use the Chain of Responsibility pattern to evaluate rules in order.
- Support URL-based filtering rules.
- Support two URL match types:
  - `REGEX`
  - `STARTS_WITH`
- Store rules in a database.
- Fan out matching requests to configured downstream endpoints.
- Return HTTP `503 Service Unavailable` when no rule matches the request.

## 3. Functional Requirements

### 3.1 HTTP Ingress

- The server shall expose an HTTP endpoint for receiving requests.
- The inbound proxy endpoint shall be open and shall not require authentication or authorization.
- The server shall preserve the full HTTP request when forwarding, except for replacing the original base URL with the configured downstream target base URL.
- The server shall preserve the request method, path, query parameters, headers, and body when forwarding.
- The server shall support common HTTP methods, including `GET`, `POST`, `PUT`, `PATCH`, and `DELETE`.
- The server shall return HTTP `503 Service Unavailable` when no rule matches the request.

### 3.2 Rule Storage

- Rules shall be persisted in a database.
- Each rule shall include:
  - A unique identifier.
  - A display name or description.
  - An enabled/disabled flag.
  - A sort order or priority value.
  - A URL match type.
  - A URL match pattern.
  - One or more downstream fanout targets.
  - A downstream timeout value.
  - Optional metadata such as creation date and update date.
- The system shall only evaluate enabled rules.
- Rule changes shall be applied without requiring application redeployment.

### 3.3 Rule Ordering

- Rules shall be evaluated in ascending sort order, priority order, or another explicitly defined deterministic ordering.
- If two rules have the same order value, the system shall use a deterministic tie-breaker, such as rule ID.
- The ordering behavior shall be documented and covered by tests.

### 3.4 Rule Matching

- The system shall support URL matching by regular expression.
- The system shall support URL matching by prefix using `STARTS_WITH`.
- Rule matching shall be performed against the request path only, for example `/api/orders/123`.
- Query strings shall not be used for rule matching.
- Invalid regular expressions shall be rejected when rules are created or updated.
- Matching shall be case-sensitive unless explicitly configured otherwise.

### 3.5 Chain of Responsibility

- The rule evaluation engine shall be implemented using the Chain of Responsibility pattern.
- Each rule shall be represented as a handler in the chain.
- Each handler shall decide whether:
  - The current request matches the rule.
  - Processing should continue to the next rule when the request does not match.
  - Processing should stop when the request matches.
  - The request should be fanned out to configured targets.
- Rule evaluation shall stop after the first matching rule.
- The chain shall be built from the sorted active rules loaded from the database.
- The implementation shall keep rule matching logic separate from Apache Camel route definitions where practical.

### 3.6 Fanout Behavior

- When a request matches a rule, the server shall forward the request to the rule's configured downstream targets.
- Fanout calls shall be dispatched asynchronously and concurrently to the matched rule's enabled downstream targets.
- The response returned to the caller shall be the first successful downstream response.
- A successful downstream response shall be an HTTP `2xx` response unless configured otherwise.
- If no downstream target returns a successful response, the response returned to the caller shall be the response from the first target in the rule's fanout target list.
- Fanout target ordering shall be deterministic and persisted.
- Downstream timeout behavior shall be configurable per rule.
- The default downstream timeout shall be 60 seconds.
- The system shall not retry failed downstream calls.
- Downstream targets shall be stored as full URLs to support targets inside or outside OpenShift.

### 3.7 Database Access

- The service shall use a database connection configured through OpenShift environment variables or mounted secrets.
- The database shall be Oracle.
- Database credentials shall not be hard-coded.
- The application shall validate database connectivity during startup or readiness checks.
- The application shall handle temporary database failures gracefully.

### 3.8 Administration

- The system shall provide a web admin panel.
- The web admin panel shall allow users to create, read, update, disable, and delete rules.
- The web admin panel shall allow users to manage fanout targets for each rule.
- The web admin panel shall allow users to configure rule sort order using drag and drop.
- The web admin panel shall allow users to configure fanout target sort order using drag and drop.
- The web admin panel shall allow users to select the URL match type as either `REGEX` or `STARTS_WITH`.
- The web admin panel shall validate rule definitions before saving them.
- The web admin panel shall validate regular expression rules before saving them.
- The web admin panel shall not require authentication or authorization.
- Rules shall be administered only through the web admin panel.
- The system shall validate rule definitions before saving them.
- The system shall prevent ambiguous or invalid rule configuration where possible.
- Rule changes saved through the web admin panel shall take effect immediately.
- The system shall not require audit history for rule changes.

### 3.9 Observability

- The service shall produce structured logs for:
  - Request receipt.
  - Rule evaluation.
  - Matched rule ID.
  - Fanout target calls.
  - Downstream failures.
  - Database errors.
- The service shall expose health and readiness endpoints for OpenShift.
- The service shall expose metrics for:
  - Request count.
  - Match count by rule.
  - No-match count.
  - Downstream latency.
  - Downstream failure count.
  - Database access errors.

## 4. Non-Functional Requirements

### 4.1 Runtime Platform

- The application shall run as a containerized Java service on OpenShift.
- The application shall support OpenShift deployment configuration using Kubernetes manifests, Helm, or Kustomize.
- The application shall expose liveness and readiness probes.
- The application shall support externalized configuration through environment variables, ConfigMaps, and Secrets.

### 4.2 Technology

- The application shall be written in Java.
- The application shall use Apache Camel for routing and integration.
- The application should use a standard Java application framework, such as Spring Boot or Quarkus, if appropriate for the project.
- The application shall use a supported JDBC driver or persistence framework for database access.

### 4.3 Security

- Secrets shall be stored using OpenShift Secrets or another approved secret-management mechanism.
- Sensitive headers and credentials shall not be logged.
- The inbound proxy endpoint shall not require authentication or authorization.
- The web admin panel shall not require authentication or authorization.
- Outbound target URLs shall be validated to reduce the risk of server-side request forgery.

### 4.4 Reliability

- The service shall tolerate downstream target failures according to configured timeout and error-handling policies.
- The service shall not retry failed downstream target calls.
- The service shall fail readiness checks if it cannot serve traffic correctly.
- The service shall avoid accepting traffic until required startup initialization is complete.
- The service shall handle malformed requests without crashing.

### 4.5 Performance

- Rule evaluation shall be efficient for the expected number of active rules.
- Regular expressions should be compiled and cached where practical.
- The system shall avoid reloading rules from the database for every request unless explicitly required.
- Rule changes shall take effect immediately after they are saved in the web admin panel.

### 4.6 Maintainability

- Rule matching, chain construction, database access, and Camel routing shall be separated into clear components.
- Unit tests shall cover rule matching and chain behavior.
- Integration tests shall cover Camel routing, database-backed rules, and fanout behavior.
- OpenShift deployment assets shall be versioned with the application.

## 5. Data Model Requirements

At minimum, the database shall support the following conceptual entities.

### 5.1 Rule

| Field | Description |
| --- | --- |
| `id` | Unique rule identifier. |
| `name` | Human-readable rule name. |
| `enabled` | Whether the rule is active. |
| `sort_order` | Rule evaluation order. |
| `match_type` | `REGEX` or `STARTS_WITH`. |
| `url_pattern` | Regex pattern or URL prefix. |
| `timeout_ms` | Downstream fanout timeout for this rule. Defaults to 60000. |
| `created_at` | Creation timestamp. |
| `updated_at` | Last update timestamp. |

### 5.2 Fanout Target

| Field | Description |
| --- | --- |
| `id` | Unique target identifier. |
| `rule_id` | Associated rule ID. |
| `target_url` | Full downstream endpoint URL. |
| `enabled` | Whether the target is active. |
| `sort_order` | Target ordering used for fanout fallback response behavior. |

## 6. Open Questions

- No open requirements questions remain at this stage.

## 7. Acceptance Criteria

- The service can be deployed to OpenShift.
- The service starts successfully using Oracle database configuration from environment variables or secrets.
- Active rules are loaded from the database and sorted deterministically.
- Requests are evaluated through a Chain of Responsibility implementation.
- Rule evaluation stops after the first matching rule.
- `REGEX` URL rules match requests correctly.
- `STARTS_WITH` URL rules match requests correctly.
- Disabled rules are ignored.
- Requests matching a rule are fanned out asynchronously and concurrently to the configured downstream targets.
- Forwarded requests preserve the original HTTP method, path, query parameters, headers, and body, replacing only the base URL with the downstream target base URL.
- Matching requests return the first successful downstream response.
- If all downstream targets fail, matching requests return the response from the first target in the fanout target list.
- Requests with no matching rule return HTTP `503 Service Unavailable`.
- Users can manage rules and fanout targets through the web admin panel without authentication or authorization.
- Users can reorder rules and fanout targets through drag and drop in the web admin panel.
- Rule changes saved through the web admin panel take effect immediately.
- Failed downstream target calls are not retried.
- Health, readiness, logs, and metrics are available for operations.
- Automated tests verify rule ordering, URL matching, chain behavior, and fanout behavior.
