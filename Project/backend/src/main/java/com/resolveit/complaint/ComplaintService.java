package com.resolveit.complaint;

import com.resolveit.assignment.ComplaintAssignmentRepository;
import com.resolveit.category.CategoryRepository;
import com.resolveit.common.Constants;
import com.resolveit.common.PageResponse;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.department.DepartmentRepository;
import com.resolveit.entity.Category;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintAssignment;
import com.resolveit.entity.Department;
import com.resolveit.entity.User;
import com.resolveit.escalation.SlaService;
import com.resolveit.exception.BadRequestException;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.notification.NotificationService;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final ComplaintAssignmentRepository assignmentRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final ComplaintAccessService accessService;
    private final ComplaintMapper complaintMapper;
    private final WorkflowService workflowService;
    private final SlaService slaService;
    private final NotificationService notificationService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            CategoryRepository categoryRepository,
                            DepartmentRepository departmentRepository,
                            DepartmentMemberRepository departmentMemberRepository,
                            ComplaintAssignmentRepository assignmentRepository,
                            ComplaintHistoryRepository historyRepository,
                            ComplaintAccessService accessService,
                            ComplaintMapper complaintMapper,
                            WorkflowService workflowService,
                            SlaService slaService,
                            NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.departmentMemberRepository = departmentMemberRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.accessService = accessService;
        this.complaintMapper = complaintMapper;
        this.workflowService = workflowService;
        this.slaService = slaService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse createComplaint(UserPrincipal principal, ComplaintDtos.CreateComplaintRequest request) {
        User creator = userRepository.findById(principal.getId())
                .filter(User::isActive)
                .orElseThrow(() -> new ForbiddenException("User account is inactive"));

        Category category = categoryRepository.findById(request.categoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new BadRequestException("INVALID_CATEGORY", "Selected category does not exist or is inactive"));

        Priority priority = request.priority() != null ? request.priority() : Priority.MEDIUM;
        Instant now = Instant.now();
        Instant dueAt = slaService.calculateDueAt(priority, now);

        Long seq = complaintRepository.nextComplaintSequence();
        int year = ZonedDateTime.now(ZoneOffset.UTC).getYear();
        String complaintNumber = String.format("CMP-%d-%06d", year, seq);

        Complaint complaint = new Complaint();
        complaint.setComplaintNumber(complaintNumber);
        complaint.setTitle(request.title().trim());
        complaint.setDescription(request.description().trim());
        complaint.setCategory(category);
        complaint.setDepartment(category.getDefaultDepartment());
        complaint.setCreator(creator);
        complaint.setPriority(priority);
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setLocation(request.location() != null ? request.location().trim() : null);
        complaint.setDueAt(dueAt);

        complaint = complaintRepository.save(complaint);

        workflowService.recordEvent(
                complaint,
                creator,
                EventType.CREATED,
                null,
                ComplaintStatus.SUBMITTED,
                "Complaint submitted",
                Map.of("priority", priority.name(), "category", category.getName())
        );

        notificationService.notifyUser(
                creator,
                complaint,
                "COMPLAINT_SUBMITTED",
                "Your complaint " + complaintNumber + " has been submitted and is awaiting review.",
                true
        );
        notificationService.notifyAdmins(
                complaint,
                "COMPLAINT_SUBMITTED",
                "New complaint " + complaintNumber + " (" + priority + ") submitted: " + complaint.getTitle(),
                false
        );

        return complaintMapper.toResponse(complaint, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintDtos.ComplaintResponse> listComplaints(
            UserPrincipal principal,
            ComplaintStatus status,
            Priority priority,
            Long categoryId,
            Long departmentId,
            Boolean escalated,
            String search,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Specification<Complaint> spec = ComplaintSpecifications.withFilters(
                status, priority, categoryId, departmentId, escalated, search, from, to
        );

        Specification<Complaint> scopeSpec = switch (principal.getRoleName()) {
            case Constants.ROLE_ADMIN -> null;
            case Constants.ROLE_MANAGER -> ComplaintSpecifications.inDepartmentsOrCreatedBy(
                    departmentMemberRepository.findActiveDepartmentIdsByUserIdAndMemberType(
                            principal.getId(), MemberType.MANAGER),
                    principal.getId()
            );
            case Constants.ROLE_EMPLOYEE -> ComplaintSpecifications.assignedToOrCreatedBy(principal.getId());
            default -> ComplaintSpecifications.createdBy(principal.getId());
        };

        if (scopeSpec != null) {
            spec = spec.and(scopeSpec);
        }

        Page<ComplaintDtos.ComplaintResponse> page = complaintRepository.findAll(spec, pageable)
                .map(c -> complaintMapper.toResponse(c, assignmentRepository.findByComplaintIdAndActiveTrue(c.getId()).orElse(null)));
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ComplaintDtos.ComplaintResponse getComplaint(Long id, UserPrincipal principal) {
        Complaint complaint = findComplaintOrThrow(id);
        accessService.requireCanView(complaint, principal);
        ComplaintAssignment activeAssignment = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, activeAssignment);
    }

    @Transactional(readOnly = true)
    public ComplaintDtos.PublicTrackingResponse trackPublic(String complaintNumber) {
        Complaint c = complaintRepository.findByComplaintNumberIgnoreCase(complaintNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        List<ComplaintDtos.PublicTrackingResponse.PublicMilestone> milestones = historyRepository
                .findByComplaintIdWithActor(c.getId())
                .stream()
                .filter(h -> h.getEventType() != EventType.COMMENT_ADDED)
                .map(h -> new ComplaintDtos.PublicTrackingResponse.PublicMilestone(
                        h.getEventType().name(),
                        h.getNewStatus(),
                        h.getCreatedAt()
                ))
                .toList();

        return new ComplaintDtos.PublicTrackingResponse(
                c.getComplaintNumber(),
                c.getStatus(),
                c.getPriority(),
                c.getCategory() != null ? c.getCategory().getName() : null,
                c.getDepartment() != null ? c.getDepartment().getName() : null,
                c.isEscalated(),
                c.getDueAt(),
                c.getResolvedAt(),
                c.getClosedAt(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                milestones
        );
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse updateComplaint(Long id, UserPrincipal principal, ComplaintDtos.UpdateComplaintRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isAdmin = Constants.ROLE_ADMIN.equals(principal.getRoleName());
        boolean isCreator = complaint.getCreator().getId().equals(principal.getId());

        if (!isAdmin && !isCreator) {
            throw new ForbiddenException("You do not have permission to edit this complaint");
        }
        if (!isAdmin && complaint.getStatus() != ComplaintStatus.SUBMITTED && complaint.getStatus() != ComplaintStatus.UNDER_REVIEW) {
            throw new ConflictException("EDIT_NOT_ALLOWED", "Complaints can only be edited before assignment");
        }
        if (complaint.getStatus() == ComplaintStatus.CLOSED || complaint.getStatus() == ComplaintStatus.REJECTED) {
            throw new ConflictException("EDIT_NOT_ALLOWED", "Closed or rejected complaints cannot be modified");
        }

        if (request.title() != null && !request.title().isBlank()) {
            complaint.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            complaint.setDescription(request.description().trim());
        }
        if (request.location() != null) {
            complaint.setLocation(request.location().trim());
        }
        if (request.categoryId() != null) {
            Category cat = categoryRepository.findById(request.categoryId())
                    .filter(Category::isActive)
                    .orElseThrow(() -> new BadRequestException("INVALID_CATEGORY", "Invalid category"));
            complaint.setCategory(cat);
        }
        if (request.priority() != null && isAdmin) {
            complaint.setPriority(request.priority());
            complaint.setDueAt(slaService.calculateDueAt(request.priority(), complaint.getCreatedAt()));
        }

        complaint = complaintRepository.save(complaint);
        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse cancelComplaint(Long id, UserPrincipal principal, ComplaintDtos.ReasonRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isAdmin = Constants.ROLE_ADMIN.equals(principal.getRoleName());
        boolean isCreator = complaint.getCreator().getId().equals(principal.getId());
        if (!isAdmin && !isCreator) {
            throw new ForbiddenException("Only the complainant or an administrator can cancel this complaint");
        }
        if (complaint.getStatus() != ComplaintStatus.SUBMITTED && complaint.getStatus() != ComplaintStatus.UNDER_REVIEW) {
            throw new ConflictException("CANCELLATION_NOT_ALLOWED", "Complaint cannot be cancelled after work has been assigned or started");
        }

        User actor = getUserOrThrow(principal.getId());
        workflowService.transitionStatus(
                complaint,
                ComplaintStatus.CLOSED,
                actor,
                EventType.CANCELLED,
                request.reason().trim(),
                Map.of("cancelledByCreator", isCreator)
        );
        complaint = complaintRepository.save(complaint);
        return complaintMapper.toResponse(complaint, null);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse closeComplaint(Long id, UserPrincipal principal, String reason) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isCreator = complaint.getCreator().getId().equals(principal.getId());
        if (!isCreator) {
            accessService.requireAdminOrDepartmentManager(complaint, principal);
        }
        User actor = getUserOrThrow(principal.getId());
        workflowService.transitionStatus(
                complaint,
                ComplaintStatus.CLOSED,
                actor,
                EventType.CLOSED,
                reason != null && !reason.isBlank() ? reason.trim() : "Confirmed closed",
                null
        );
        complaint = complaintRepository.save(complaint);
        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse reopenComplaint(Long id, UserPrincipal principal, ComplaintDtos.ReasonRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isAdmin = Constants.ROLE_ADMIN.equals(principal.getRoleName());
        boolean isCreator = complaint.getCreator().getId().equals(principal.getId());

        if (!isAdmin && !isCreator) {
            throw new ForbiddenException("Only the complainant or an administrator can reopen this complaint");
        }

        if (!isAdmin && complaint.getStatus() == ComplaintStatus.RESOLVED && complaint.getResolvedAt() != null) {
            int windowDays = slaService.getReopenWindowDays(complaint.getPriority());
            Instant deadline = complaint.getResolvedAt().plus(windowDays, ChronoUnit.DAYS);
            if (Instant.now().isAfter(deadline)) {
                throw new ConflictException("REOPEN_WINDOW_EXPIRED",
                        "Reopening window of " + windowDays + " day(s) has expired");
            }
        }

        User actor = getUserOrThrow(principal.getId());
        workflowService.transitionStatus(
                complaint,
                ComplaintStatus.REOPENED,
                actor,
                EventType.REOPENED,
                request.reason().trim(),
                null
        );
        complaint = complaintRepository.save(complaint);

        if (complaint.getDepartment() != null) {
            notificationService.notifyDepartmentManagers(
                    complaint.getDepartment().getId(),
                    complaint,
                    "COMPLAINT_REOPENED",
                    "Complaint " + complaint.getComplaintNumber() + " was reopened: " + request.reason().trim(),
                    true
            );
        }
        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        if (active != null) {
            notificationService.notifyUser(
                    active.getEmployee(),
                    complaint,
                    "COMPLAINT_REOPENED",
                    "Assigned complaint " + complaint.getComplaintNumber() + " was reopened.",
                    true
            );
        }
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse rejectComplaint(Long id, UserPrincipal principal, ComplaintDtos.ReasonRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        accessService.requireAdminOrDepartmentManager(complaint, principal);
        User actor = getUserOrThrow(principal.getId());
        workflowService.transitionStatus(
                complaint,
                ComplaintStatus.REJECTED,
                actor,
                EventType.REJECTED,
                request.reason().trim(),
                null
        );
        complaint = complaintRepository.save(complaint);
        return complaintMapper.toResponse(complaint, null);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse routeToDepartment(Long id, UserPrincipal principal, ComplaintDtos.RouteComplaintRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        Department department = departmentRepository.findById(request.departmentId())
                .filter(Department::isActive)
                .orElseThrow(() -> new BadRequestException("INVALID_DEPARTMENT", "Department not found or inactive"));

        User actor = getUserOrThrow(principal.getId());
        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setDepartment(department);

        if (oldStatus == ComplaintStatus.SUBMITTED) {
            workflowService.transitionStatus(
                    complaint,
                    ComplaintStatus.UNDER_REVIEW,
                    actor,
                    EventType.ROUTED_DEPARTMENT,
                    request.reason() != null ? request.reason().trim() : "Routed to " + department.getName(),
                    Map.of("departmentId", department.getId(), "departmentName", department.getName())
            );
        } else {
            workflowService.recordEvent(
                    complaint,
                    actor,
                    EventType.ROUTED_DEPARTMENT,
                    oldStatus,
                    oldStatus,
                    request.reason() != null ? request.reason().trim() : "Routed to " + department.getName(),
                    Map.of("departmentId", department.getId(), "departmentName", department.getName())
            );
        }

        complaint = complaintRepository.save(complaint);

        notificationService.notifyDepartmentManagers(
                department.getId(),
                complaint,
                "DEPARTMENT_ROUTED",
                "Complaint " + complaint.getComplaintNumber() + " has been routed to " + department.getName() + ".",
                true
        );

        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse startWork(Long id, UserPrincipal principal) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isAdmin = Constants.ROLE_ADMIN.equals(principal.getRoleName());
        boolean isAssigned = accessService.isAssignedEmployee(complaint, principal.getId());
        if (!isAdmin && !isAssigned) {
            throw new ForbiddenException("Only the assigned employee can start work on this complaint");
        }
        User actor = getUserOrThrow(principal.getId());
        workflowService.transitionStatus(
                complaint,
                ComplaintStatus.IN_PROGRESS,
                actor,
                EventType.STATUS_CHANGED,
                "Investigation started",
                null
        );
        complaint = complaintRepository.save(complaint);
        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse resolveComplaint(Long id, UserPrincipal principal, ComplaintDtos.ResolveComplaintRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isAssigned = accessService.isAssignedEmployee(complaint, principal.getId());
        if (!isAssigned) {
            accessService.requireAdminOrDepartmentManager(complaint, principal);
        }
        User actor = getUserOrThrow(principal.getId());
        complaint.setResolutionNotes(request.resolutionNotes().trim());
        workflowService.transitionStatus(
                complaint,
                ComplaintStatus.RESOLVED,
                actor,
                EventType.RESOLVED,
                request.resolutionNotes().trim(),
                null
        );
        complaint = complaintRepository.save(complaint);

        if (complaint.getDepartment() != null) {
            notificationService.notifyDepartmentManagers(
                    complaint.getDepartment().getId(),
                    complaint,
                    "COMPLAINT_RESOLVED",
                    "Complaint " + complaint.getComplaintNumber() + " has been marked RESOLVED.",
                    true
            );
        }
        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional
    public ComplaintDtos.ComplaintResponse transitionStatus(Long id, UserPrincipal principal, ComplaintDtos.StatusChangeRequest request) {
        Complaint complaint = findComplaintOrThrow(id);
        boolean isAssigned = accessService.isAssignedEmployee(complaint, principal.getId());
        if (!isAssigned) {
            accessService.requireAdminOrDepartmentManager(complaint, principal);
        }
        if (request.targetStatus() == ComplaintStatus.REJECTED && (request.reason() == null || request.reason().isBlank())) {
            throw new BadRequestException("REASON_REQUIRED", "A reason is required when rejecting a complaint");
        }
        User actor = getUserOrThrow(principal.getId());
        workflowService.transitionStatus(
                complaint,
                request.targetStatus(),
                actor,
                EventType.STATUS_CHANGED,
                request.reason(),
                null
        );
        complaint = complaintRepository.save(complaint);
        ComplaintAssignment active = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, active);
    }

    @Transactional(readOnly = true)
    public List<ComplaintHistoryResponse> getHistory(Long id, UserPrincipal principal) {
        Complaint complaint = findComplaintOrThrow(id);
        accessService.requireCanView(complaint, principal);
        boolean includeInternal = accessService.canViewInternalDetails(complaint, principal);
        return historyRepository.findByComplaintIdWithActor(id).stream()
                .map(h -> ComplaintHistoryResponse.from(h, includeInternal))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintDtos.DuplicateCandidateResponse> findPotentialDuplicates(Long categoryId) {
        Instant since = Instant.now().minus(14, ChronoUnit.DAYS);
        return complaintRepository.findRecentPotentialDuplicates(categoryId, since, PageRequest.of(0, 5))
                .stream()
                .map(c -> new ComplaintDtos.DuplicateCandidateResponse(
                        c.getId(),
                        c.getComplaintNumber(),
                        c.getTitle(),
                        c.getStatus(),
                        c.getLocation(),
                        c.getCreatedAt()
                ))
                .toList();
    }

    public Complaint findComplaintOrThrow(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

