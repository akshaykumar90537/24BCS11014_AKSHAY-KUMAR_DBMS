package com.resolveit.complaint;

import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ComplaintDtos {
    private ComplaintDtos() {}

    public record CreateComplaintRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10000) String description,
            @NotNull Long categoryId,
            Priority priority,
            @Size(max = 255) String location
    ) {}

    public record UpdateComplaintRequest(
            @Size(max = 200) String title,
            @Size(max = 10000) String description,
            Long categoryId,
            Priority priority,
            @Size(max = 255) String location
    ) {}

    public record ReasonRequest(
            @NotBlank @Size(max = 2000) String reason
    ) {}

    public record ResolveComplaintRequest(
            @NotBlank @Size(max = 5000) String resolutionNotes
    ) {}

    public record StatusChangeRequest(
            @NotNull ComplaintStatus targetStatus,
            @Size(max = 2000) String reason
    ) {}

    public record RouteComplaintRequest(
            @NotNull Long departmentId,
            @Size(max = 2000) String reason
    ) {}

    public record ComplaintResponse(
            Long id,
            String complaintNumber,
            String title,
            String description,
            ComplaintStatus status,
            Priority priority,
            Long categoryId,
            String categoryName,
            Long departmentId,
            String departmentName,
            Long creatorId,
            String creatorName,
            Long assignedEmployeeId,
            String assignedEmployeeName,
            Long activeAssignmentId,
            String location,
            Instant dueAt,
            Instant resolvedAt,
            Instant closedAt,
            boolean escalated,
            Instant escalatedAt,
            String resolutionNotes,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record PublicTrackingResponse(
            String complaintNumber,
            ComplaintStatus status,
            Priority priority,
            String categoryName,
            String departmentName,
            boolean escalated,
            Instant dueAt,
            Instant resolvedAt,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt,
            List<PublicMilestone> milestones
    ) {
        public record PublicMilestone(
                String eventType,
                ComplaintStatus status,
                Instant timestamp
        ) {}
    }

    public record DuplicateCandidateResponse(
            Long id,
            String complaintNumber,
            String title,
            ComplaintStatus status,
            String location,
            Instant createdAt
    ) {}
}

