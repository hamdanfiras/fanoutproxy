package com.example.fanoutproxy.rules;

import com.example.fanoutproxy.domain.MatchType;
import java.util.List;
import java.util.regex.Pattern;

public record RuleDefinition(
        Long id,
        String name,
        int sortOrder,
        MatchType matchType,
        String urlPattern,
        int timeoutMs,
        List<TargetDefinition> targets
) {
    public Pattern compiledPattern() {
        return Pattern.compile(urlPattern);
    }
}
