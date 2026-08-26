# Fanout Proxy

Java Spring Boot and Apache Camel fanout proxy server with an Oracle-backed rule store and a no-auth web admin panel.

## Runtime

- Admin panel: `GET /admin`
- Proxy endpoint: `/proxy/**`
- Rule matching uses the path after `/proxy`. For example, `/proxy/api/orders/1` matches rules against `/api/orders/1`.
- No-match response: HTTP `503 Service Unavailable`
- Fanout dispatch: async and concurrent
- Caller response: first downstream HTTP `2xx`; if none succeed, response from the first configured target
- Downstream retry: none
- Default rule timeout: `60000` ms
- Target servers are stored separately and assigned to rules through `FANOUT_RULE_TARGET`

## Database

The Oracle schema uses these main objects:

- `FANOUT_RULE`
- `TARGET_SERVER`
- `FANOUT_RULE_TARGET`

`TARGET_SERVER` stores reusable downstream server definitions. `FANOUT_RULE_TARGET` is the many-to-many relationship table and stores per-rule assignment state, including `ENABLED` and `SORT_ORDER`.

## Configuration

Set these environment variables in OpenShift:

```text
DB_URL=jdbc:oracle:thin:@//oracle-host:1521/service
DB_USERNAME=fanout
DB_PASSWORD=...
FANOUT_PROXY_PREFIX=/proxy
```

## Build

```sh
mvn test
mvn package
```

Build the container after packaging:

```sh
docker build -t fanoutproxy:latest .
```

Apply the OpenShift manifests after pushing the image to the registry used by the cluster:

```sh
oc apply -f k8s/deployment.yaml
```
