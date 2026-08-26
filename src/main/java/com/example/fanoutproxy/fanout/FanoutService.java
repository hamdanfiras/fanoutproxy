package com.example.fanoutproxy.fanout;

import com.example.fanoutproxy.proxy.ProxyRequest;
import com.example.fanoutproxy.proxy.ProxyResponse;
import com.example.fanoutproxy.rules.RuleDefinition;
import com.example.fanoutproxy.rules.TargetDefinition;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FanoutService {

    private static final Logger log = LoggerFactory.getLogger(FanoutService.class);

    private final ProducerTemplate producerTemplate;
    private final ExecutorService executorService;

    public FanoutService(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdown();
    }

    public ProxyResponse fanout(ProxyRequest request, @Header("fanoutRule") RuleDefinition rule) {
        if (rule.targets().isEmpty()) {
            return new ProxyResponse(503, Map.of(), "Matched rule has no enabled fanout targets".getBytes(StandardCharsets.UTF_8));
        }

        CompletionService<IndexedResponse> completionService = new ExecutorCompletionService<>(executorService);
        for (int i = 0; i < rule.targets().size(); i++) {
            TargetDefinition target = rule.targets().get(i);
            completionService.submit(callTarget(i, request, rule, target));
        }

        List<IndexedResponse> responses = new ArrayList<>();
        for (int i = 0; i < rule.targets().size(); i++) {
            try {
                IndexedResponse response = completionService.take().get();
                responses.add(response);
                if (response.response().successful()) {
                    return response.response();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return new ProxyResponse(503, Map.of(), "Fanout interrupted".getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                log.warn("Fanout target call failed", ex);
            }
        }

        return responses.stream()
                .filter(response -> response.index() == 0)
                .findFirst()
                .map(IndexedResponse::response)
                .orElseGet(() -> new ProxyResponse(503, Map.of(), "All fanout targets failed".getBytes(StandardCharsets.UTF_8)));
    }

    private Callable<IndexedResponse> callTarget(int index, ProxyRequest request, RuleDefinition rule, TargetDefinition target) {
        return () -> {
            try {
                ProxyResponse response = callTarget(request, rule, target);
                return new IndexedResponse(index, response);
            } catch (Exception ex) {
                log.warn("Target {} failed for rule {}", target.targetUrl(), rule.id(), ex);
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                return new IndexedResponse(index, new ProxyResponse(502, Map.of(), message.getBytes(StandardCharsets.UTF_8)));
            }
        };
    }

    private ProxyResponse callTarget(ProxyRequest request, RuleDefinition rule, TargetDefinition target) {
        TargetUri targetUri = buildTargetUri(target.targetUrl(), request.path(), request.queryString());
        String endpointUri = targetUri.endpointUri(rule.timeoutMs());

        Exchange exchange = producerTemplate.request(endpointUri, camelExchange -> {
            Message in = camelExchange.getIn();
            in.setBody(request.body());
            copyRequestHeaders(request.headers(), in);
            in.setHeader(Exchange.HTTP_METHOD, request.method());
            if (targetUri.query() != null && !targetUri.query().isBlank()) {
                in.setHeader(Exchange.HTTP_QUERY, targetUri.query());
            }
        });

        Message out = exchange.getMessage();
        int status = Objects.requireNonNullElse(out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class), 500);
        byte[] body = out.getBody(byte[].class);
        if (body == null) {
            String textBody = out.getBody(String.class);
            body = textBody == null ? new byte[0] : textBody.getBytes(StandardCharsets.UTF_8);
        }
        return new ProxyResponse(status, copyResponseHeaders(out.getHeaders()), body);
    }

    private void copyRequestHeaders(Map<String, List<String>> headers, Message in) {
        headers.forEach((name, values) -> {
            if (name == null || values == null || isProtocolManagedRequestHeader(name)) {
                return;
            }
            in.setHeader(name, values.size() == 1 ? values.get(0) : String.join(",", values));
        });
    }

    private Map<String, List<String>> copyResponseHeaders(Map<String, Object> headers) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (name == null || value == null || isInternalOrProtocolResponseHeader(name)) {
                return;
            }
            copied.put(name, List.of(String.valueOf(value)));
        });
        return copied;
    }

    private boolean isProtocolManagedRequestHeader(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals("host")
                || normalized.equals("content-length")
                || normalized.equals("transfer-encoding")
                || normalized.equals("connection");
    }

    private boolean isInternalOrProtocolResponseHeader(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.startsWith("camel")
                || normalized.equals("content-length")
                || normalized.equals("transfer-encoding")
                || normalized.equals("connection");
    }

    private TargetUri buildTargetUri(String targetBaseUrl, String requestPath, String requestQuery) {
        URI targetBase = URI.create(targetBaseUrl);
        String basePath = targetBase.getRawPath() == null ? "" : targetBase.getRawPath();
        String joinedPath = joinPaths(basePath, requestPath);
        String endpoint = targetBase.getScheme() + "://" + targetBase.getRawAuthority() + joinedPath;

        String query = mergeQuery(targetBase.getRawQuery(), requestQuery);
        return new TargetUri(endpoint, query);
    }

    private String joinPaths(String basePath, String requestPath) {
        String safeBase = basePath == null ? "" : basePath;
        String safeRequest = requestPath == null || requestPath.isBlank() ? "/" : requestPath;
        if (!safeRequest.startsWith("/")) {
            safeRequest = "/" + safeRequest;
        }
        if (safeBase.isBlank() || "/".equals(safeBase)) {
            return safeRequest;
        }
        if (safeBase.endsWith("/") && safeRequest.startsWith("/")) {
            return safeBase.substring(0, safeBase.length() - 1) + safeRequest;
        }
        return safeBase + safeRequest;
    }

    private String mergeQuery(String targetQuery, String requestQuery) {
        if (targetQuery == null || targetQuery.isBlank()) {
            return requestQuery;
        }
        if (requestQuery == null || requestQuery.isBlank()) {
            return targetQuery;
        }
        return targetQuery + "&" + requestQuery;
    }

    private record IndexedResponse(int index, ProxyResponse response) {
    }

    private record TargetUri(String endpoint, String query) {

        String endpointUri(int timeoutMs) {
            Map<String, String> options = new HashMap<>();
            options.put("throwExceptionOnFailure", "false");
            options.put("bridgeEndpoint", "true");
            options.put("copyHeaders", "true");
            options.put("connectTimeout", String.valueOf(timeoutMs));
            options.put("responseTimeout", String.valueOf(timeoutMs));

            StringBuilder uri = new StringBuilder(endpoint);
            uri.append(endpoint.contains("?") ? "&" : "?");
            options.forEach((key, value) -> uri.append(key)
                    .append("=")
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                    .append("&"));
            uri.setLength(uri.length() - 1);
            return uri.toString();
        }
    }
}
