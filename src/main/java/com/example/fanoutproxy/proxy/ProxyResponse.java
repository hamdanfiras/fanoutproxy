package com.example.fanoutproxy.proxy;

import java.util.List;
import java.util.Map;

public record ProxyResponse(
        int status,
        Map<String, List<String>> headers,
        byte[] body
) {
    public boolean successful() {
        return status >= 200 && status <= 299;
    }
}
