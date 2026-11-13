package com.resolveit.report;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.Priority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Reports & Analytics")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated summary metrics by status, priority, SLA compliance and feedback")
    public ApiResponse<ReportDtos.SummaryReport> summary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) ComplaintStatus status
    ) {
        return ApiResponse.ok(reportService.getSummary(principal, from, to, departmentId, categoryId, priority, status));
    }

    @GetMapping("/by-department")
    @Operation(summary = "Get volume, pending counts and SLA metrics per department")
    public ApiResponse<List<ReportDtos.DepartmentMetrics>> byDepartment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId
    ) {
        return ApiResponse.ok(reportService.getByDepartment(principal, from, to, departmentId));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get daily or monthly complaint submission and resolution trends")
    public ApiResponse<List<ReportDtos.TrendPoint>> trends(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "monthly") String groupBy,
            @RequestParam(required = false) Long departmentId
    ) {
        return ApiResponse.ok(reportService.getTrends(principal, from, to, groupBy, departmentId));
    }

    @GetMapping("/resolution-time")
    @Operation(summary = "Get average and median resolution time by department or category")
    public ApiResponse<List<ReportDtos.ResolutionTimeMetric>> resolutionTime(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "department") String groupBy,
            @RequestParam(required = false) Long departmentId
    ) {
        return ApiResponse.ok(reportService.getResolutionTimes(principal, from, to, groupBy, departmentId));
    }

    @GetMapping("/export")
    @Operation(summary = "Export filtered report as CSV")
    public ResponseEntity<String> exportCsv(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) ComplaintStatus status
    ) {
        String csv = reportService.exportCsv(principal, from, to, departmentId, categoryId, priority, status);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resolveit-report.csv\"")
                .body(csv);
    }
}

