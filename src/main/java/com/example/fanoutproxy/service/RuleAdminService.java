package com.example.fanoutproxy.service;

import com.example.fanoutproxy.config.FanoutProperties;
import com.example.fanoutproxy.domain.FanoutRule;
import com.example.fanoutproxy.domain.FanoutRuleTarget;
import com.example.fanoutproxy.domain.MatchType;
import com.example.fanoutproxy.domain.TargetServer;
import com.example.fanoutproxy.repository.FanoutRuleRepository;
import com.example.fanoutproxy.repository.FanoutRuleTargetRepository;
import com.example.fanoutproxy.repository.TargetServerRepository;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleAdminService {

    private final FanoutRuleRepository ruleRepository;
    private final FanoutRuleTargetRepository ruleTargetRepository;
    private final TargetServerRepository targetServerRepository;
    private final RuleSnapshotService snapshotService;
    private final FanoutProperties properties;

    public RuleAdminService(
            FanoutRuleRepository ruleRepository,
            FanoutRuleTargetRepository ruleTargetRepository,
            TargetServerRepository targetServerRepository,
            RuleSnapshotService snapshotService,
            FanoutProperties properties
    ) {
        this.ruleRepository = ruleRepository;
        this.ruleTargetRepository = ruleTargetRepository;
        this.targetServerRepository = targetServerRepository;
        this.snapshotService = snapshotService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<FanoutRule> allRules() {
        return ruleRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public FanoutRule getRule(Long id) {
        return ruleRepository.findWithTargetsById(id).orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TargetServer> allTargetServers() {
        return targetServerRepository.findAllByOrderByNameAscIdAsc();
    }

    @Transactional(readOnly = true)
    public TargetServer getTargetServer(Long id) {
        return targetServerRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Target server not found: " + id));
    }

    @Transactional
    public FanoutRule saveRule(Long id, String name, boolean enabled, int sortOrder, MatchType matchType, String urlPattern, Integer timeoutMs) {
        validateRule(matchType, urlPattern);

        FanoutRule rule = id == null
                ? new FanoutRule()
                : ruleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
        rule.setName(name);
        rule.setEnabled(enabled);
        rule.setSortOrder(sortOrder);
        rule.setMatchType(matchType);
        rule.setUrlPattern(urlPattern);
        rule.setTimeoutMs(timeoutMs == null || timeoutMs <= 0 ? properties.getDefaultTimeoutMs() : timeoutMs);

        FanoutRule saved = ruleRepository.save(rule);
        snapshotService.refresh();
        return saved;
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
        snapshotService.refresh();
    }

    @Transactional
    public void reorderRules(String orderedIds) {
        List<Long> ids = parseIds(orderedIds);
        for (int i = 0; i < ids.size(); i++) {
            FanoutRule rule = getRule(ids.get(i));
            rule.setSortOrder(i);
        }
        snapshotService.refresh();
    }

    @Transactional
    public TargetServer saveTargetServer(Long id, String name, String targetUrl, boolean enabled) {
        validateTargetUrl(targetUrl);

        TargetServer targetServer = id == null
                ? new TargetServer()
                : targetServerRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Target server not found: " + id));
        targetServer.setName(name);
        targetServer.setTargetUrl(targetUrl);
        targetServer.setEnabled(enabled);

        TargetServer saved = targetServerRepository.save(targetServer);
        snapshotService.refresh();
        return saved;
    }

    @Transactional
    public void deleteTargetServer(Long id) {
        targetServerRepository.deleteById(id);
        snapshotService.refresh();
    }

    @Transactional
    public FanoutRuleTarget saveRuleTarget(Long ruleId, Long ruleTargetId, Long targetServerId, boolean enabled, int sortOrder) {
        FanoutRule rule = getRule(ruleId);
        TargetServer targetServer = getTargetServer(targetServerId);

        FanoutRuleTarget ruleTarget = ruleTargetId == null
                ? new FanoutRuleTarget()
                : ruleTargetRepository.findById(ruleTargetId)
                .orElseThrow(() -> new EntityNotFoundException("Rule target not found: " + ruleTargetId));
        ruleTarget.setRule(rule);
        ruleTarget.setTargetServer(targetServer);
        ruleTarget.setEnabled(enabled);
        ruleTarget.setSortOrder(sortOrder);

        FanoutRuleTarget saved = ruleTargetRepository.save(ruleTarget);
        snapshotService.refresh();
        return saved;
    }

    @Transactional
    public void deleteRuleTarget(Long ruleTargetId) {
        ruleTargetRepository.deleteById(ruleTargetId);
        snapshotService.refresh();
    }

    @Transactional
    public void reorderTargets(Long ruleId, String orderedIds) {
        List<Long> ids = parseIds(orderedIds);
        for (int i = 0; i < ids.size(); i++) {
            Long ruleTargetId = ids.get(i);
            FanoutRuleTarget ruleTarget = ruleTargetRepository.findById(ruleTargetId)
                    .orElseThrow(() -> new EntityNotFoundException("Rule target not found: " + ruleTargetId));
            if (!ruleTarget.getRule().getId().equals(ruleId)) {
                throw new IllegalArgumentException("Rule target " + ruleTarget.getId() + " does not belong to rule " + ruleId);
            }
            ruleTarget.setSortOrder(i);
        }
        snapshotService.refresh();
    }

    private void validateRule(MatchType matchType, String urlPattern) {
        if (matchType == null) {
            throw new IllegalArgumentException("Match type is required");
        }
        if (urlPattern == null || urlPattern.isBlank()) {
            throw new IllegalArgumentException("URL pattern is required");
        }
        if (matchType == MatchType.REGEX) {
            try {
                Pattern.compile(urlPattern);
            } catch (PatternSyntaxException ex) {
                throw new IllegalArgumentException("Invalid regular expression: " + ex.getDescription(), ex);
            }
        }
    }

    private void validateTargetUrl(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("Target URL is required");
        }
        URI uri;
        try {
            uri = URI.create(targetUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Target URL is invalid", ex);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Target URL must start with http:// or https://");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Target URL must include a host");
        }
    }

    private List<Long> parseIds(String orderedIds) {
        if (orderedIds == null || orderedIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(orderedIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .toList();
    }
}
