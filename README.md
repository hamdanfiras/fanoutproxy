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
