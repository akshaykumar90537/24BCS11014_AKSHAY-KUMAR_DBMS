package com.resolveit.complaint;

import com.resolveit.common.ApiResponse;
import com.resolveit.common.PageResponse;
import com.resolveit.escalation.SlaPolicyDto;
import com.resolveit.escalation.SlaService;
import com.resolveit.escalation.UpdateSlaPolicyRequest;
import com.resolveit.report.ReportDtos;
import com.resolveit.report.ReportService;
import com.resolveit.security.UserPrincipal;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.Priority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration")
public class AdminController {

    private final ComplaintService complaintService;
    private final ReportService reportService;
    private final SlaService slaService;

    public AdminController(ComplaintService complaintService,
                           ReportService reportService,
                           SlaService slaService) {
        this.complaintService = complaintService;
        this.reportService = reportService;
        this.slaService = slaService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get global counts, department metrics and monthly trends")
    public ApiResponse<ReportDtos.AdminDashboardResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(reportService.getAdminDashboard(principal));
    }

    @GetMapping("/complaints")
    @Operation(summary = "Search all organization complaints with filters")
    public ApiResponse<PageResponse<ComplaintDtos.ComplaintResponse>> searchComplaints(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean escalated,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return ApiResponse.ok(complaintService.listComplaints(
                principal, status, priority, categoryId, departmentId, escalated, search, from, to, pageable));
    }

    @PostMapping("/complaints/{id}/route")
    @Operation(summary = "Route a complaint to a responsible department")
    public ApiResponse<ComplaintDtos.ComplaintResponse> routeComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.RouteComplaintRequest request
    ) {
        return ApiResponse.ok("Complaint routed to department",
                complaintService.routeToDepartment(id, principal, request));
    }

    @GetMapping("/sla-policies")
    @Operation(summary = "List configurable SLA targets and reopen window per priority")
    public ApiResponse<List<SlaPolicyDto>> listSlaPolicies() {
        return ApiResponse.ok(slaService.listPolicies());
    }

    @PutMapping("/sla-policies/{priority}")
    @Operation(summary = "Update SLA target hours and reopen window for a priority")
    public ApiResponse<SlaPolicyDto> updateSlaPolicy(
            @PathVariable Priority priority,
            @Valid @RequestBody UpdateSlaPolicyRequest request
    ) {
        return ApiResponse.ok("SLA policy updated", slaService.updatePolicy(priority, request));
    }
}

