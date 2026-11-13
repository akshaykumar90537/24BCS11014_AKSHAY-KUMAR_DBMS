package com.resolveit.notification;

import com.resolveit.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long complaintId,
        String complaintNumber,
        String type,
        String message,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getComplaint() != null ? n.getComplaint().getId() : null,
                n.getComplaint() != null ? n.getComplaint().getComplaintNumber() : null,
                n.getType(),
                n.getMessage(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}

