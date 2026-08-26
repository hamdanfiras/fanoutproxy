package com.example.fanoutproxy.repository;

import com.example.fanoutproxy.domain.FanoutRuleTarget;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanoutRuleTargetRepository extends JpaRepository<FanoutRuleTarget, Long> {

    List<FanoutRuleTarget> findByRuleIdOrderBySortOrderAscIdAsc(Long ruleId);
}
