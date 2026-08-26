package com.example.fanoutproxy.repository;

import com.example.fanoutproxy.domain.TargetServer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetServerRepository extends JpaRepository<TargetServer, Long> {

    List<TargetServer> findAllByOrderByNameAscIdAsc();
}
