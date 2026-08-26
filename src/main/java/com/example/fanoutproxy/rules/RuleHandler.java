package com.example.fanoutproxy.rules;

import com.example.fanoutproxy.proxy.ProxyRequest;
import java.util.Optional;

public interface RuleHandler {

    Optional<RuleDefinition> handle(ProxyRequest request);
}
