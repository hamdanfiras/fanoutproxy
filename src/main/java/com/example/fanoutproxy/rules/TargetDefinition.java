package com.example.fanoutproxy.rules;

public record TargetDefinition(
        Long id,
        String targetUrl,
        int sortOrder
) {
}
