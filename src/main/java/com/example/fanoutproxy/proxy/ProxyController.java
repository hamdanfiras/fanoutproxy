package com.example.fanoutproxy.proxy;

import com.example.fanoutproxy.rules.RuleDefinition;
import com.example.fanoutproxy.rules.RuleEngine;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProxyController {

    private final RuleEngine ruleEngine;
    private final ProducerTemplate producerTemplate;

    public ProxyController(RuleEngine ruleEngine, ProducerTemplate producerTemplate) {
        this.ruleEngine = ruleEngine;
        this.producerTemplate = producerTemplate;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest servletRequest, @RequestBody(required = false) byte[] body) {
        ProxyRequest request = toProxyRequest(servletRequest, body == null ? new byte[0] : body);
        RuleDefinition rule = ruleEngine.firstMatch(request).orElse(null);
        if (rule == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No fanout rule matched the request path".getBytes(StandardCharsets.UTF_8));
        }

        ProxyResponse response = producerTemplate.requestBodyAndHeader(
                "direct:fanout",
                request,
                "fanoutRule",
                rule,
                ProxyResponse.class
        );
        return toResponseEntity(response);
    }

    private ProxyRequest toProxyRequest(HttpServletRequest servletRequest, byte[] body) {
        String path = requestPath(servletRequest);
        return new ProxyRequest(
                servletRequest.getMethod(),
                path,
                servletRequest.getQueryString(),
                headers(servletRequest),
                body
        );
    }

    private String requestPath(HttpServletRequest servletRequest) {
        String requestUri = servletRequest.getRequestURI();
        String contextPath = servletRequest.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        return requestUri == null || requestUri.isBlank() ? "/" : requestUri;
    }

    private Map<String, List<String>> headers(HttpServletRequest servletRequest) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(servletRequest.getHeaderNames())
                .forEach(name -> headers.put(name, Collections.list(servletRequest.getHeaders(name))));
        return headers;
    }

    private ResponseEntity<byte[]> toResponseEntity(ProxyResponse response) {
        HttpHeaders headers = new HttpHeaders();
        response.headers().forEach(headers::put);
        return new ResponseEntity<>(response.body(), headers, HttpStatus.valueOf(response.status()));
    }
}
