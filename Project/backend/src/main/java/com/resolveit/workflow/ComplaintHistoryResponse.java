package com.resolveit.workflow;

import com.resolveit.entity.ComplaintHistory;

import java.time.Instant;
import java.util.Map;

public record ComplaintHistoryResponse(
        Long id,
        EventType eventType,
        ComplaintStatus oldStatus,
        ComplaintStatus newStatus,
        String reason,
        Long actorId,
        String actorName,
        String actorRole,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static ComplaintHistoryResponse from(ComplaintHistory history, boolean includeInternalDetails) {
        String actorName = history.getActor() != null ? history.getActor().getName() : "System";
        String actorRole = history.getActor() != null ? history.getActor().getRole().getName() : "SYSTEM";
        Long actorId = history.getActor() != null ? history.getActor().getId() : null;
        return new ComplaintHistoryResponse(
                history.getId(),
                history.getEventType(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getReason(),
                actorId,
                actorName,
                actorRole,
                includeInternalDetails ? history.getMetadata() : null,
                history.getCreatedAt()
        );
    }
}

