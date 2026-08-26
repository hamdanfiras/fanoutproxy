package com.example.fanoutproxy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;

@Entity
@Table(
        name = "FANOUT_RULE_TARGET",
        uniqueConstraints = @UniqueConstraint(name = "UK_FANOUT_RULE_TARGET", columnNames = {"RULE_ID", "TARGET_SERVER_ID"})
)
public class FanoutRuleTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RULE_ID", nullable = false)
    private FanoutRule rule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARGET_SERVER_ID", nullable = false)
    private TargetServer targetServer;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Min(0)
    @Column(name = "SORT_ORDER", nullable = false)
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FanoutRule getRule() {
        return rule;
    }

    public void setRule(FanoutRule rule) {
        this.rule = rule;
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(TargetServer targetServer) {
        this.targetServer = targetServer;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
