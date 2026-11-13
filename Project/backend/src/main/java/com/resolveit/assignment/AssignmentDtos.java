package com.resolveit.assignment;

import com.resolveit.entity.ComplaintAssignment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AssignmentDtos {
    private AssignmentDtos() {}

    public record AssignEmployeeRequest(
            @NotNull Long employeeId,
            @Size(max = 2000) String reason
    ) {}

    public record ReassignEmployeeRequest(
            @NotNull Long employeeId,
            @NotBlank @Size(max = 2000) String reason
    ) {}

    public record AssignmentResponse(
            Long id,
            Long complaintId,
            Long employeeId,
            String employeeName,
            String employeeEmail,
            Long assignedById,
            String assignedByName,
            Instant assignedAt,
            Instant unassignedAt,
            String reason,
            boolean active
    ) {
        public static AssignmentResponse from(ComplaintAssignment ca) {
            return new AssignmentResponse(
                    ca.getId(),
                    ca.getComplaint().getId(),
                    ca.getEmployee().getId(),
                    ca.getEmployee().getName(),
                    ca.getEmployee().getEmail(),
                    ca.getAssignedBy().getId(),
                    ca.getAssignedBy().getName(),
                    ca.getAssignedAt(),
                    ca.getUnassignedAt(),
                    ca.getReason(),
                    ca.isActive()
            );
        }
    }

    public record EmployeeWorkloadDto(
            Long employeeId,
            String employeeName,
            String employeeEmail,
            long activeCount
    ) {}

    public record DepartmentQueueResponse(
            List<EmployeeWorkloadDto> workload,
            com.resolveit.common.PageResponse<com.resolveit.complaint.ComplaintDtos.ComplaintResponse> complaints
    ) {}
}

