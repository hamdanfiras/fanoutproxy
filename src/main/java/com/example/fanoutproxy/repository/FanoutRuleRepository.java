package com.example.fanoutproxy.repository;

import com.example.fanoutproxy.domain.FanoutRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FanoutRuleRepository extends JpaRepository<FanoutRule, Long> {

    @EntityGraph(attributePaths = {"ruleTargets", "ruleTargets.targetServer"})
    List<FanoutRule> findAllByOrderBySortOrderAscIdAsc();

    @EntityGraph(attributePaths = {"ruleTargets", "ruleTargets.targetServer"})
    List<FanoutRule> findByEnabledTrueOrderBySortOrderAscIdAsc();

    @EntityGraph(attributePaths = {"ruleTargets", "ruleTargets.targetServer"})
    @Query("select r from FanoutRule r where r.id = :id")
    Optional<FanoutRule> findWithTargetsById(Long id);
}
