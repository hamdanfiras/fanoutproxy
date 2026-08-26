package com.example.fanoutproxy.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.fanoutproxy.domain.MatchType;
import com.example.fanoutproxy.proxy.ProxyRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleChainFactoryTest {

    private final RuleChainFactory factory = new RuleChainFactory();

    @Test
    void returnsFirstMatchingRuleOnly() {
        RuleDefinition first = rule(1L, 0, MatchType.STARTS_WITH, "/api");
        RuleDefinition second = rule(2L, 1, MatchType.STARTS_WITH, "/api/orders");

        RuleHandler chain = factory.build(List.of(first, second));

        assertThat(chain.handle(request("/api/orders/123"))).contains(first);
    }

    @Test
    void supportsRegexRules() {
        RuleDefinition regex = rule(1L, 0, MatchType.REGEX, "/api/orders/[0-9]+");

        RuleHandler chain = factory.build(List.of(regex));

        assertThat(chain.handle(request("/api/orders/123"))).contains(regex);
        assertThat(chain.handle(request("/api/orders/new"))).isEmpty();
    }

    @Test
    void supportsStartsWithRules() {
        RuleDefinition prefix = rule(1L, 0, MatchType.STARTS_WITH, "/api/orders");

        RuleHandler chain = factory.build(List.of(prefix));

        assertThat(chain.handle(request("/api/orders/123"))).contains(prefix);
        assertThat(chain.handle(request("/api/customers/123"))).isEmpty();
    }

    private RuleDefinition rule(Long id, int sortOrder, MatchType matchType, String pattern) {
        return new RuleDefinition(id, "rule-" + id, sortOrder, matchType, pattern, 60000, List.of());
    }

    private ProxyRequest request(String path) {
        return new ProxyRequest("GET", path, null, Map.of(), new byte[0]);
    }
}
