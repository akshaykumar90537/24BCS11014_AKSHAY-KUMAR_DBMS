package com.resolveit.escalation;

import com.resolveit.entity.SlaPolicy;
import com.resolveit.workflow.Priority;

import java.time.Instant;

public record SlaPolicyDto(
        Priority priority,
        int targetHours,
        int reopenWindowDays,
        Instant updatedAt
) {
    public static SlaPolicyDto from(SlaPolicy policy) {
        return new SlaPolicyDto(
                policy.getPriority(),
                policy.getTargetHours(),
                policy.getReopenWindowDays(),
                policy.getUpdatedAt()
        );
    }
}

