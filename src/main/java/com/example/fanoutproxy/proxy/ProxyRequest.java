package com.example.fanoutproxy.proxy;

import java.util.List;
import java.util.Map;

public record ProxyRequest(
        String method,
        String path,
        String queryString,
        Map<String, List<String>> headers,
        byte[] body
) {
}
