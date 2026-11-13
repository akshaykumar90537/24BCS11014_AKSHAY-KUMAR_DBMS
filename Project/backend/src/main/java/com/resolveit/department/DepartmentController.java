package com.resolveit.department;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "List active departments (or all for admin)")
    public ApiResponse<List<DepartmentDtos.DepartmentResponse>> list(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(departmentService.listDepartments(includeInactive, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a department")
    public ApiResponse<DepartmentDtos.DepartmentResponse> create(
            @Valid @RequestBody DepartmentDtos.CreateDepartmentRequest request
    ) {
        return ApiResponse.ok("Department created", departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a department")
    public ApiResponse<DepartmentDtos.DepartmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDtos.UpdateDepartmentRequest request
    ) {
        return ApiResponse.ok("Department updated", departmentService.updateDepartment(id, request));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List active members of a department")
    public ApiResponse<List<DepartmentDtos.DepartmentMemberResponse>> listMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(departmentService.listMembers(id, principal));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add or reactivate a department manager/employee membership")
    public ApiResponse<DepartmentDtos.DepartmentMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDtos.AddMemberRequest request
    ) {
        return ApiResponse.ok("Member added to department", departmentService.addMember(id, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate department membership while preserving audit history")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        departmentService.deactivateMember(id, userId);
    }
}

