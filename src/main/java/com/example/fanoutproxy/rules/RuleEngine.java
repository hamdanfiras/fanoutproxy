package com.example.fanoutproxy.rules;

import com.example.fanoutproxy.proxy.ProxyRequest;
import com.example.fanoutproxy.service.RuleSnapshotService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RuleEngine {

    private final RuleSnapshotService snapshotService;
    private final RuleChainFactory chainFactory;

    public RuleEngine(RuleSnapshotService snapshotService, RuleChainFactory chainFactory) {
        this.snapshotService = snapshotService;
        this.chainFactory = chainFactory;
    }

    public Optional<RuleDefinition> firstMatch(ProxyRequest request) {
        RuleHandler chain = chainFactory.build(snapshotService.currentRules());
        return chain.handle(request);
    }
}
