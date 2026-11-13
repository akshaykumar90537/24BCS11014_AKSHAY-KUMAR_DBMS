package com.resolveit.complaint;

import com.resolveit.common.ApiResponse;
import com.resolveit.common.PageResponse;
import com.resolveit.security.UserPrincipal;
import com.resolveit.workflow.ComplaintHistoryResponse;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.Priority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "Complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new complaint")
    public ApiResponse<ComplaintDtos.ComplaintResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.CreateComplaintRequest request
    ) {
        return ApiResponse.ok("Complaint submitted successfully", complaintService.createComplaint(principal, request));
    }

    @GetMapping
    @Operation(summary = "List visible complaints with pagination and filters")
    public ApiResponse<PageResponse<ComplaintDtos.ComplaintResponse>> list(
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort);
        return ApiResponse.ok(complaintService.listComplaints(
                principal, status, priority, categoryId, departmentId, escalated, search, from, to, pageable));
    }

    @GetMapping("/potential-duplicates")
    @Operation(summary = "Check recent open complaints in the same category before submitting")
    public ApiResponse<List<ComplaintDtos.DuplicateCandidateResponse>> duplicates(@RequestParam Long categoryId) {
        return ApiResponse.ok(complaintService.findPotentialDuplicates(categoryId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role-scoped complaint details")
    public ApiResponse<ComplaintDtos.ComplaintResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(complaintService.getComplaint(id, principal));
    }

    @GetMapping("/track/{number}")
    @Operation(summary = "Public safe tracking status lookup by complaint number")
    public ApiResponse<ComplaintDtos.PublicTrackingResponse> trackPublic(@PathVariable String number) {
        return ApiResponse.ok(complaintService.trackPublic(number));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update editable complaint fields before assignment")
    public ApiResponse<ComplaintDtos.ComplaintResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.UpdateComplaintRequest request
    ) {
        return ApiResponse.ok("Complaint updated", complaintService.updateComplaint(id, principal, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a complaint before assignment/investigation")
    public ApiResponse<ComplaintDtos.ComplaintResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.ReasonRequest request
    ) {
        return ApiResponse.ok("Complaint cancelled", complaintService.cancelComplaint(id, principal, request));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a resolved complaint")
    public ApiResponse<ComplaintDtos.ComplaintResponse> close(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) ComplaintDtos.ReasonRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        return ApiResponse.ok("Complaint closed", complaintService.closeComplaint(id, principal, reason));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen an eligible resolved complaint within policy window")
    public ApiResponse<ComplaintDtos.ComplaintResponse> reopen(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.ReasonRequest request
    ) {
        return ApiResponse.ok("Complaint reopened", complaintService.reopenComplaint(id, principal, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reject an invalid or duplicate complaint with mandatory reason")
    public ApiResponse<ComplaintDtos.ComplaintResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.ReasonRequest request
    ) {
        return ApiResponse.ok("Complaint rejected", complaintService.rejectComplaint(id, principal, request));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start work on an assigned complaint (ASSIGNED -> IN_PROGRESS)")
    public ApiResponse<ComplaintDtos.ComplaintResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Complaint work started", complaintService.startWork(id, principal));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Submit resolution notes and move complaint to RESOLVED")
    public ApiResponse<ComplaintDtos.ComplaintResponse> resolve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.ResolveComplaintRequest request
    ) {
        return ApiResponse.ok("Complaint resolved", complaintService.resolveComplaint(id, principal, request));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    @Operation(summary = "Perform a policy-validated status transition")
    public ApiResponse<ComplaintDtos.ComplaintResponse> transitionStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintDtos.StatusChangeRequest request
    ) {
        return ApiResponse.ok("Status updated", complaintService.transitionStatus(id, principal, request));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "View complaint audit history timeline")
    public ApiResponse<List<ComplaintHistoryResponse>> history(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(complaintService.getHistory(id, principal));
    }
}

