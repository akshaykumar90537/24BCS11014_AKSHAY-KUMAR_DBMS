package com.resolveit.escalation;

import com.resolveit.entity.SlaPolicy;
import com.resolveit.workflow.Priority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SlaService {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaService(SlaPolicyRepository slaPolicyRepository) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Transactional(readOnly = true)
    public Instant calculateDueAt(Priority priority, Instant fromInstant) {
        int hours = slaPolicyRepository.findByPriority(priority)
                .map(SlaPolicy::getTargetHours)
                .orElse(defaultHoursFor(priority));
        return fromInstant.plus(hours, ChronoUnit.HOURS);
    }

    @Transactional(readOnly = true)
    public int getReopenWindowDays(Priority priority) {
        return slaPolicyRepository.findByPriority(priority)
                .map(SlaPolicy::getReopenWindowDays)
                .orElse(7);
    }

    @Transactional(readOnly = true)
    public List<SlaPolicyDto> listPolicies() {
        return slaPolicyRepository.findAllByOrderByTargetHoursAsc().stream()
                .map(SlaPolicyDto::from)
                .toList();
    }

    @Transactional
    public SlaPolicyDto updatePolicy(Priority priority, UpdateSlaPolicyRequest request) {
        SlaPolicy policy = slaPolicyRepository.findByPriority(priority)
                .orElseGet(() -> {
                    SlaPolicy p = new SlaPolicy();
                    p.setPriority(priority);
                    return p;
                });
        policy.setTargetHours(request.targetHours());
        policy.setReopenWindowDays(request.reopenWindowDays());
        return SlaPolicyDto.from(slaPolicyRepository.save(policy));
    }

    private int defaultHoursFor(Priority priority) {
        return switch (priority) {
            case CRITICAL -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 120;
        };
    }
}

