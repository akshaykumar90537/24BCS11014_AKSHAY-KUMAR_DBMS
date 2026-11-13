package com.resolveit.escalation;

import com.resolveit.entity.SlaPolicy;
import com.resolveit.workflow.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(Priority priority);
    List<SlaPolicy> findAllByOrderByTargetHoursAsc();
}

