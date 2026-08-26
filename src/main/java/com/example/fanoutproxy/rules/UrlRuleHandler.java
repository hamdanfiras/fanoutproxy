package com.example.fanoutproxy.rules;

import com.example.fanoutproxy.domain.MatchType;
import com.example.fanoutproxy.proxy.ProxyRequest;
import java.util.Optional;
import java.util.regex.Pattern;

public class UrlRuleHandler implements RuleHandler {

    private final RuleDefinition rule;
    private final Pattern compiledPattern;
    private final RuleHandler next;

    public UrlRuleHandler(RuleDefinition rule, RuleHandler next) {
        this.rule = rule;
        this.compiledPattern = rule.matchType() == MatchType.REGEX ? Pattern.compile(rule.urlPattern()) : null;
        this.next = next;
    }

    @Override
    public Optional<RuleDefinition> handle(ProxyRequest request) {
        if (matches(request.path())) {
            return Optional.of(rule);
        }
        return next == null ? Optional.empty() : next.handle(request);
    }

    private boolean matches(String path) {
        return switch (rule.matchType()) {
            case STARTS_WITH -> path.startsWith(rule.urlPattern());
            case REGEX -> compiledPattern.matcher(path).matches();
        };
    }
}
