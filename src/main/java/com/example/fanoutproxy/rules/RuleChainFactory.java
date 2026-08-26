package com.example.fanoutproxy.rules;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleChainFactory {

    public RuleHandler build(List<RuleDefinition> rules) {
        RuleHandler next = null;
        for (int i = rules.size() - 1; i >= 0; i--) {
            next = new UrlRuleHandler(rules.get(i), next);
        }
        return next == null ? request -> java.util.Optional.empty() : next;
    }
}
