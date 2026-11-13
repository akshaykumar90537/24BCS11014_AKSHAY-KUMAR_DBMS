package com.resolveit.assignment;

import com.resolveit.common.Constants;
import com.resolveit.common.PageResponse;
import com.resolveit.complaint.ComplaintAccessService;
import com.resolveit.complaint.ComplaintDtos;
import com.resolveit.complaint.ComplaintMapper;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.complaint.ComplaintSpecifications;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintAssignment;
import com.resolveit.entity.User;
import com.resolveit.exception.BadRequestException;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.notification.NotificationService;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintAssignmentRepository assignmentRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final UserRepository userRepository;
    private final ComplaintAccessService accessService;
    private final ComplaintMapper complaintMapper;
    private final WorkflowService workflowService;
    private final NotificationService notificationService;

    public AssignmentService(ComplaintRepository complaintRepository,
                             ComplaintAssignmentRepository assignmentRepository,
                             DepartmentMemberRepository departmentMemberRepository,
                             UserRepository userRepository,
                             ComplaintAccessService accessService,
                             ComplaintMapper complaintMapper,
                             WorkflowService workflowService,
                             NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.assignmentRepository = assignmentRepository;
        this.departmentMemberRepository = departmentMemberRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.complaintMapper = complaintMapper;
        this.workflowService = workflowService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AssignmentDtos.AssignmentResponse assignEmployee(Long complaintId,
                                                            UserPrincipal principal,
                                                            AssignmentDtos.AssignEmployeeRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (complaint.getDepartment() == null) {
            throw new ConflictException("DEPARTMENT_REQUIRED", "Complaint must be routed to a department before assigning an employee");
        }
        accessService.requireAdminOrDepartmentManager(complaint, principal);

        User employee = validateDepartmentAssignee(complaint, request.employeeId());
        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (complaint.getStatus() == ComplaintStatus.CLOSED || complaint.getStatus() == ComplaintStatus.REJECTED) {
            throw new ConflictException("COMPLAINT_TERMINAL", "Cannot assign a closed or rejected complaint");
        }

        Instant now = Instant.now();
        assignmentRepository.deactivateActiveByComplaintId(complaintId, now);

        ComplaintAssignment assignment = new ComplaintAssignment();
        assignment.setComplaint(complaint);
        assignment.setEmployee(employee);
        assignment.setAssignedBy(actor);
        assignment.setAssignedAt(now);
        assignment.setReason(request.reason());
        assignment.setActive(true);
        assignment = assignmentRepository.save(assignment);

        if (complaint.getStatus() != ComplaintStatus.ASSIGNED) {
            workflowService.transitionStatus(
                    complaint,
                    ComplaintStatus.ASSIGNED,
                    actor,
                    EventType.ASSIGNED,
                    request.reason() != null ? request.reason() : "Assigned to " + employee.getName(),
                    Map.of("employeeId", employee.getId(), "employeeName", employee.getName())
            );
        } else {
            workflowService.recordEvent(
                    complaint,
                    actor,
                    EventType.ASSIGNED,
                    complaint.getStatus(),
                    complaint.getStatus(),
                    request.reason(),
                    Map.of("employeeId", employee.getId(), "employeeName", employee.getName())
            );
        }
        complaintRepository.save(complaint);

        notificationService.notifyUser(
                employee,
                complaint,
                "EMPLOYEE_ASSIGNED",
                "You have been assigned to complaint " + complaint.getComplaintNumber() + ": " + complaint.getTitle(),
                true
        );

        return AssignmentDtos.AssignmentResponse.from(assignment);
    }

    @Transactional
    public AssignmentDtos.AssignmentResponse reassignEmployee(Long complaintId,
                                                              Long assignmentId,
                                                              UserPrincipal principal,
                                                              AssignmentDtos.ReassignEmployeeRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireAdminOrDepartmentManager(complaint, principal);

        if (complaint.getDepartment() == null) {
            throw new ConflictException("DEPARTMENT_REQUIRED", "Complaint has no responsible department");
        }

        ComplaintAssignment existing = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getComplaint().getId().equals(complaintId) && a.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Active assignment not found"));

        User newEmployee = validateDepartmentAssignee(complaint, request.employeeId());
        if (existing.getEmployee().getId().equals(newEmployee.getId())) {
            throw new ConflictException("SAME_ASSIGNEE", "Complaint is already assigned to this employee");
        }

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instant now = Instant.now();
        existing.setActive(false);
        existing.setUnassignedAt(now);
        assignmentRepository.saveAndFlush(existing);

        ComplaintAssignment next = new ComplaintAssignment();
        next.setComplaint(complaint);
        next.setEmployee(newEmployee);
        next.setAssignedBy(actor);
        next.setAssignedAt(now);
        next.setReason(request.reason().trim());
        next.setActive(true);
        next = assignmentRepository.save(next);

        ComplaintStatus oldStatus = complaint.getStatus();
        if (oldStatus == ComplaintStatus.IN_PROGRESS || oldStatus == ComplaintStatus.REOPENED) {
            workflowService.transitionStatus(
                    complaint,
                    ComplaintStatus.ASSIGNED,
                    actor,
                    EventType.REASSIGNED,
                    request.reason().trim(),
                    Map.of(
                            "previousEmployeeId", existing.getEmployee().getId(),
                            "newEmployeeId", newEmployee.getId(),
                            "newEmployeeName", newEmployee.getName()
                    )
            );
        } else {
            workflowService.recordEvent(
                    complaint,
                    actor,
                    EventType.REASSIGNED,
                    oldStatus,
                    oldStatus,
                    request.reason().trim(),
                    Map.of(
                            "previousEmployeeId", existing.getEmployee().getId(),
                            "newEmployeeId", newEmployee.getId(),
                            "newEmployeeName", newEmployee.getName()
                    )
            );
        }
        complaintRepository.save(complaint);

        notificationService.notifyUser(
                newEmployee,
                complaint,
                "EMPLOYEE_ASSIGNED",
                "Complaint " + complaint.getComplaintNumber() + " has been reassigned to you.",
                true
        );

        return AssignmentDtos.AssignmentResponse.from(next);
    }

    @Transactional(readOnly = true)
    public List<AssignmentDtos.AssignmentResponse> getAssignmentHistory(Long complaintId, UserPrincipal principal) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireCanView(complaint, principal);
        return assignmentRepository.findByComplaintIdOrderByAssignedAtDesc(complaintId).stream()
                .map(AssignmentDtos.AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintDtos.ComplaintResponse> getEmployeeAssignedComplaints(UserPrincipal principal, Pageable pageable) {
        Page<ComplaintDtos.ComplaintResponse> page = assignmentRepository
                .findActiveAssignmentsByEmployeeId(principal.getId(), pageable)
                .map(ca -> complaintMapper.toResponse(ca.getComplaint(), ca));
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public AssignmentDtos.DepartmentQueueResponse getManagerDepartmentQueue(
            UserPrincipal principal,
            Long departmentId,
            ComplaintStatus status,
            Pageable pageable
    ) {
        List<Long> managerDepts = departmentMemberRepository.findActiveDepartmentIdsByUserIdAndMemberType(
                principal.getId(), MemberType.MANAGER);

        if (managerDepts.isEmpty()) {
            throw new ForbiddenException("Manager is not assigned to any active department");
        }

        List<Long> targetDepts = managerDepts;
        if (departmentId != null) {
            if (!managerDepts.contains(departmentId)) {
                throw new ForbiddenException("You do not manage the requested department");
            }
            targetDepts = List.of(departmentId);
        }

        Long primaryDeptId = targetDepts.get(0);
        List<AssignmentDtos.EmployeeWorkloadDto> workload = departmentMemberRepository
                .findByDepartmentIdAndActiveTrue(primaryDeptId)
                .stream()
                .map(dm -> {
                    long count = assignmentRepository.countActiveWorkloadByDepartment(primaryDeptId).stream()
                            .filter(row -> ((Number) row[0]).longValue() == dm.getUser().getId())
                            .mapToLong(row -> ((Number) row[3]).longValue())
                            .findFirst()
                            .orElse(0L);
                    return new AssignmentDtos.EmployeeWorkloadDto(
                            dm.getUser().getId(),
                            dm.getUser().getName(),
                            dm.getUser().getEmail(),
                            count
                    );
                })
                .toList();

        Specification<Complaint> spec = ComplaintSpecifications.inDepartments(targetDepts)
                .and(ComplaintSpecifications.withFilters(status, null, null, null, null, null, null, null));
        Page<ComplaintDtos.ComplaintResponse> complaints = complaintRepository.findAll(spec, pageable)
                .map(c -> complaintMapper.toResponse(c, assignmentRepository.findByComplaintIdAndActiveTrue(c.getId()).orElse(null)));

        return new AssignmentDtos.DepartmentQueueResponse(workload, PageResponse.from(complaints));
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintDtos.ComplaintResponse> getManagerOverdue(UserPrincipal principal, Pageable pageable) {
        List<Long> managerDepts = departmentMemberRepository.findActiveDepartmentIdsByUserIdAndMemberType(
                principal.getId(), MemberType.MANAGER);
        if (managerDepts.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0, true, true);
        }
        Page<ComplaintDtos.ComplaintResponse> page = complaintRepository
                .findOverdueByDepartmentIds(managerDepts, Instant.now(), pageable)
                .map(c -> complaintMapper.toResponse(c, assignmentRepository.findByComplaintIdAndActiveTrue(c.getId()).orElse(null)));
        return PageResponse.from(page);
    }

    private User validateDepartmentAssignee(Complaint complaint, Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .filter(User::isActive)
                .orElseThrow(() -> new BadRequestException("INVALID_EMPLOYEE", "Selected employee does not exist or is inactive"));

        String role = employee.getRole().getName();
        if (!Constants.ROLE_EMPLOYEE.equals(role) && !Constants.ROLE_MANAGER.equals(role)) {
            throw new BadRequestException("INVALID_ASSIGNEE_ROLE", "Selected user is not staff");
        }

        boolean member = departmentMemberRepository.existsByDepartmentIdAndUserIdAndActiveTrue(
                complaint.getDepartment().getId(), employee.getId());
        if (!member) {
            throw new ForbiddenException("Cannot assign staff outside the complaint's responsible department");
        }
        return employee;
    }
}

