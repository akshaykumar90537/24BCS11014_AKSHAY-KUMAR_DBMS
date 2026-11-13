package com.resolveit.complaint;

import com.resolveit.assignment.ComplaintAssignmentRepository;
import com.resolveit.common.Constants;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.entity.Complaint;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.security.UserPrincipal;
import com.resolveit.workflow.MemberType;
import org.springframework.stereotype.Service;

@Service
public class ComplaintAccessService {

    private final DepartmentMemberRepository departmentMemberRepository;
    private final ComplaintAssignmentRepository assignmentRepository;

    public ComplaintAccessService(DepartmentMemberRepository departmentMemberRepository,
                                  ComplaintAssignmentRepository assignmentRepository) {
        this.departmentMemberRepository = departmentMemberRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public boolean isStaff(UserPrincipal principal) {
        String role = principal.getRoleName();
        return Constants.ROLE_ADMIN.equals(role)
                || Constants.ROLE_MANAGER.equals(role)
                || Constants.ROLE_EMPLOYEE.equals(role);
    }

    public boolean canViewComplaint(Complaint complaint, UserPrincipal principal) {
        String role = principal.getRoleName();
        if (Constants.ROLE_ADMIN.equals(role)) {
            return true;
        }
        if (complaint.getCreator().getId().equals(principal.getId())) {
            return true;
        }
        if (Constants.ROLE_MANAGER.equals(role) && complaint.getDepartment() != null) {
            return departmentMemberRepository.existsByDepartmentIdAndUserIdAndMemberTypeAndActiveTrue(
                    complaint.getDepartment().getId(), principal.getId(), MemberType.MANAGER);
        }
        if (Constants.ROLE_EMPLOYEE.equals(role)) {
            return assignmentRepository.existsByComplaintIdAndEmployeeIdAndActiveTrue(
                    complaint.getId(), principal.getId());
        }
        return false;
    }

    public void requireCanView(Complaint complaint, UserPrincipal principal) {
        if (!canViewComplaint(complaint, principal)) {
            throw new ForbiddenException("You do not have permission to access this complaint");
        }
    }

    public boolean canAddInternalComment(Complaint complaint, UserPrincipal principal) {
        String role = principal.getRoleName();
        if (Constants.ROLE_ADMIN.equals(role)) {
            return true;
        }
        if (Constants.ROLE_MANAGER.equals(role) && complaint.getDepartment() != null) {
            return departmentMemberRepository.existsByDepartmentIdAndUserIdAndMemberTypeAndActiveTrue(
                    complaint.getDepartment().getId(), principal.getId(), MemberType.MANAGER);
        }
        if (Constants.ROLE_EMPLOYEE.equals(role)) {
            return assignmentRepository.existsByComplaintIdAndEmployeeIdAndActiveTrue(
                    complaint.getId(), principal.getId());
        }
        return false;
    }

    public boolean canViewInternalDetails(Complaint complaint, UserPrincipal principal) {
        return canAddInternalComment(complaint, principal);
    }

    public void requireAdminOrDepartmentManager(Complaint complaint, UserPrincipal principal) {
        String role = principal.getRoleName();
        if (Constants.ROLE_ADMIN.equals(role)) {
            return;
        }
        if (Constants.ROLE_MANAGER.equals(role) && complaint.getDepartment() != null) {
            boolean isDeptManager = departmentMemberRepository.existsByDepartmentIdAndUserIdAndMemberTypeAndActiveTrue(
                    complaint.getDepartment().getId(), principal.getId(), MemberType.MANAGER);
            if (isDeptManager) {
                return;
            }
        }
        throw new ForbiddenException("Only an administrator or the department manager can perform this action");
    }

    public boolean isAssignedEmployee(Complaint complaint, Long userId) {
        return assignmentRepository.existsByComplaintIdAndEmployeeIdAndActiveTrue(complaint.getId(), userId);
    }
}

