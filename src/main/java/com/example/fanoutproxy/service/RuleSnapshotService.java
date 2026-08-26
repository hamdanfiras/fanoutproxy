package com.example.fanoutproxy.service;

import com.example.fanoutproxy.domain.FanoutRule;
import com.example.fanoutproxy.domain.FanoutRuleTarget;
import com.example.fanoutproxy.repository.FanoutRuleRepository;
import com.example.fanoutproxy.rules.RuleDefinition;
import com.example.fanoutproxy.rules.TargetDefinition;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleSnapshotService {

    private final FanoutRuleRepository ruleRepository;
    private final AtomicReference<List<RuleDefinition>> rules = new AtomicReference<>(List.of());

    public RuleSnapshotService(FanoutRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @PostConstruct
    @Transactional(readOnly = true)
    public void refresh() {
        List<RuleDefinition> loaded = ruleRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toDefinition)
                .toList();
        rules.set(loaded);
    }

    public List<RuleDefinition> currentRules() {
        return rules.get();
    }

    private RuleDefinition toDefinition(FanoutRule rule) {
        List<TargetDefinition> targets = rule.getRuleTargets()
                .stream()
                .filter(FanoutRuleTarget::isEnabled)
                .filter(target -> target.getTargetServer().isEnabled())
                .sorted(Comparator.comparingInt(FanoutRuleTarget::getSortOrder).thenComparing(FanoutRuleTarget::getId))
                .map(target -> new TargetDefinition(
                        target.getId(),
                        target.getTargetServer().getTargetUrl(),
                        target.getSortOrder()
                ))
                .toList();

        return new RuleDefinition(
                rule.getId(),
                rule.getName(),
                rule.getSortOrder(),
                rule.getMatchType(),
                rule.getUrlPattern(),
                rule.getTimeoutMs(),
                targets
        );
    }
}
