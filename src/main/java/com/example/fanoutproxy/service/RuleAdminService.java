package com.example.fanoutproxy.service;

import com.example.fanoutproxy.config.FanoutProperties;
import com.example.fanoutproxy.domain.FanoutRule;
import com.example.fanoutproxy.domain.FanoutTarget;
import com.example.fanoutproxy.domain.MatchType;
import com.example.fanoutproxy.repository.FanoutRuleRepository;
import com.example.fanoutproxy.repository.FanoutTargetRepository;
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
    private final FanoutTargetRepository targetRepository;
    private final RuleSnapshotService snapshotService;
    private final FanoutProperties properties;

    public RuleAdminService(
            FanoutRuleRepository ruleRepository,
            FanoutTargetRepository targetRepository,
            RuleSnapshotService snapshotService,
            FanoutProperties properties
    ) {
        this.ruleRepository = ruleRepository;
        this.targetRepository = targetRepository;
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
    public FanoutTarget saveTarget(Long ruleId, Long targetId, String targetUrl, boolean enabled, int sortOrder) {
        validateTargetUrl(targetUrl);

        FanoutRule rule = getRule(ruleId);
        FanoutTarget target = targetId == null
                ? new FanoutTarget()
                : targetRepository.findById(targetId).orElseThrow(() -> new EntityNotFoundException("Target not found: " + targetId));
        target.setRule(rule);
        target.setTargetUrl(targetUrl);
        target.setEnabled(enabled);
        target.setSortOrder(sortOrder);

        FanoutTarget saved = targetRepository.save(target);
        snapshotService.refresh();
        return saved;
    }

    @Transactional
    public void deleteTarget(Long targetId) {
        targetRepository.deleteById(targetId);
        snapshotService.refresh();
    }

    @Transactional
    public void reorderTargets(Long ruleId, String orderedIds) {
        List<Long> ids = parseIds(orderedIds);
        for (int i = 0; i < ids.size(); i++) {
            Long targetId = ids.get(i);
            FanoutTarget target = targetRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("Target not found: " + targetId));
            if (!target.getRule().getId().equals(ruleId)) {
                throw new IllegalArgumentException("Target " + target.getId() + " does not belong to rule " + ruleId);
            }
            target.setSortOrder(i);
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
        URI uri = URI.create(targetUrl);
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
