package com.resolveit.user;

import com.resolveit.entity.User;

import java.time.Instant;
import java.util.List;

public record UserDto(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        Instant createdAt,
        List<UserDepartmentDto> departments
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName(),
                user.isActive(),
                user.getCreatedAt(),
                List.of()
        );
    }

    public static UserDto from(User user, List<UserDepartmentDto> departments) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().getName(),
                user.isActive(),
                user.getCreatedAt(),
                departments
        );
    }

    public record UserDepartmentDto(
            Long departmentId,
            String departmentName,
            String memberType
    ) {}
}

