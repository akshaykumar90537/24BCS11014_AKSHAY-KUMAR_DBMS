package com.resolveit.assignment;

import com.resolveit.common.ApiResponse;
import com.resolveit.common.PageResponse;
import com.resolveit.complaint.ComplaintDtos;
import com.resolveit.security.UserPrincipal;
import com.resolveit.workflow.ComplaintStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Assignments & Queues")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/complaints/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign a department employee to a complaint")
    public ApiResponse<AssignmentDtos.AssignmentResponse> assign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignmentDtos.AssignEmployeeRequest request
    ) {
        return ApiResponse.ok("Employee assigned", assignmentService.assignEmployee(id, principal, request));
    }

    @PostMapping("/complaints/{id}/assignments/{assignmentId}/reassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Close active assignment and reassign complaint to another department employee")
    public ApiResponse<AssignmentDtos.AssignmentResponse> reassign(
            @PathVariable Long id,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignmentDtos.ReassignEmployeeRequest request
    ) {
        return ApiResponse.ok("Complaint reassigned", assignmentService.reassignEmployee(id, assignmentId, principal, request));
    }

    @GetMapping("/complaints/{id}/assignments")
    @Operation(summary = "List assignment history for a complaint")
    public ApiResponse<List<AssignmentDtos.AssignmentResponse>> history(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(assignmentService.getAssignmentHistory(id, principal));
    }

    @GetMapping("/employee/complaints")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "List complaints currently assigned to the authenticated employee")
    public ApiResponse<PageResponse<ComplaintDtos.ComplaintResponse>> employeeQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ApiResponse.ok(assignmentService.getEmployeeAssignedComplaints(principal, pageable));
    }

    @GetMapping("/manager/department-queue")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List department complaint queue and staff workload")
    public ApiResponse<AssignmentDtos.DepartmentQueueResponse> managerQueue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return ApiResponse.ok(assignmentService.getManagerDepartmentQueue(principal, departmentId, status, pageable));
    }

    @GetMapping("/manager/overdue")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List overdue or escalated complaints in manager's department")
    public ApiResponse<PageResponse<ComplaintDtos.ComplaintResponse>> managerOverdue(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ApiResponse.ok(assignmentService.getManagerOverdue(principal, pageable));
    }
}

