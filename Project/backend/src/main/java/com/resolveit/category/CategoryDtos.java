package com.resolveit.category;

import com.resolveit.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {
    private CategoryDtos() {}

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description,
            Long defaultDepartmentId
    ) {}

    public record UpdateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 2000) String description,
            Long defaultDepartmentId,
            boolean active
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String description,
            Long defaultDepartmentId,
            String defaultDepartmentName,
            boolean active
    ) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(
                    c.getId(),
                    c.getName(),
                    c.getDescription(),
                    c.getDefaultDepartment() != null ? c.getDefaultDepartment().getId() : null,
                    c.getDefaultDepartment() != null ? c.getDefaultDepartment().getName() : null,
                    c.isActive()
            );
        }
    }
}

