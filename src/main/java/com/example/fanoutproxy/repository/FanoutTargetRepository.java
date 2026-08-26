package com.example.fanoutproxy.repository;

import com.example.fanoutproxy.domain.FanoutTarget;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FanoutTargetRepository extends JpaRepository<FanoutTarget, Long> {

    List<FanoutTarget> findByRuleIdOrderBySortOrderAscIdAsc(Long ruleId);
}
