package com.resolveit.department;

import com.resolveit.entity.Department;
import com.resolveit.entity.DepartmentMember;
import com.resolveit.workflow.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class DepartmentDtos {
    private DepartmentDtos() {}

    public record CreateDepartmentRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description
    ) {}

    public record UpdateDepartmentRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description,
            boolean active
    ) {}

    public record AddMemberRequest(
            @NotNull Long userId,
            @NotNull MemberType memberType
    ) {}

    public record DepartmentMemberResponse(
            Long id,
            Long userId,
            String userName,
            String userEmail,
            String userRole,
            MemberType memberType,
            boolean active,
            Instant joinedAt
    ) {
        public static DepartmentMemberResponse from(DepartmentMember dm) {
            return new DepartmentMemberResponse(
                    dm.getId(),
                    dm.getUser().getId(),
                    dm.getUser().getName(),
                    dm.getUser().getEmail(),
                    dm.getUser().getRole().getName(),
                    dm.getMemberType(),
                    dm.isActive(),
                    dm.getJoinedAt()
            );
        }
    }

    public record DepartmentResponse(
            Long id,
            String name,
            String description,
            boolean active,
            Instant createdAt,
            List<DepartmentMemberResponse> members
    ) {
        public static DepartmentResponse from(Department d, List<DepartmentMemberResponse> members) {
            return new DepartmentResponse(
                    d.getId(),
                    d.getName(),
                    d.getDescription(),
                    d.isActive(),
                    d.getCreatedAt(),
                    members
            );
        }
    }
}

