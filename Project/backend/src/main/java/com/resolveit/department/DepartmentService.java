package com.resolveit.department;

import com.resolveit.common.Constants;
import com.resolveit.entity.Department;
import com.resolveit.entity.DepartmentMember;
import com.resolveit.entity.User;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMemberRepository memberRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             DepartmentMemberRepository memberRepository,
                             UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentResponse> listDepartments(boolean includeInactive, UserPrincipal principal) {
        boolean isAdmin = principal != null && Constants.ROLE_ADMIN.equals(principal.getRoleName());
        List<Department> list = (includeInactive && isAdmin)
                ? departmentRepository.findAllByOrderByNameAsc()
                : departmentRepository.findByActiveTrueOrderByNameAsc();

        return list.stream()
                .map(d -> DepartmentDtos.DepartmentResponse.from(
                        d,
                        isAdmin ? memberRepository.findByDepartmentIdAndActiveTrue(d.getId()).stream()
                                .map(DepartmentDtos.DepartmentMemberResponse::from)
                                .toList() : List.of()
                ))
                .toList();
    }

    @Transactional
    public DepartmentDtos.DepartmentResponse createDepartment(DepartmentDtos.CreateDepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("DEPARTMENT_EXISTS", "A department with this name already exists");
        }
        Department dept = new Department();
        dept.setName(request.name().trim());
        dept.setDescription(request.description());
        dept.setActive(true);
        dept = departmentRepository.save(dept);
        return DepartmentDtos.DepartmentResponse.from(dept, List.of());
    }

    @Transactional
    public DepartmentDtos.DepartmentResponse updateDepartment(Long id, DepartmentDtos.UpdateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        departmentRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("DEPARTMENT_EXISTS", "A department with this name already exists");
                });
        dept.setName(request.name().trim());
        dept.setDescription(request.description());
        dept.setActive(request.active());
        dept = departmentRepository.save(dept);
        List<DepartmentDtos.DepartmentMemberResponse> members = memberRepository
                .findByDepartmentIdAndActiveTrue(id)
                .stream()
                .map(DepartmentDtos.DepartmentMemberResponse::from)
                .toList();
        return DepartmentDtos.DepartmentResponse.from(dept, members);
    }

    @Transactional
    public DepartmentDtos.DepartmentMemberResponse addMember(Long departmentId, DepartmentDtos.AddMemberRequest request) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DepartmentMember member = memberRepository.findByDepartmentIdAndUserId(departmentId, user.getId())
                .orElseGet(DepartmentMember::new);
        member.setDepartment(dept);
        member.setUser(user);
        member.setMemberType(request.memberType());
        member.setActive(true);
        return DepartmentDtos.DepartmentMemberResponse.from(memberRepository.save(member));
    }

    @Transactional
    public void deactivateMember(Long departmentId, Long userId) {
        DepartmentMember member = memberRepository.findByDepartmentIdAndUserId(departmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Department membership not found"));
        member.setActive(false);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentMemberResponse> listMembers(Long departmentId, UserPrincipal principal) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found");
        }
        boolean isAdmin = Constants.ROLE_ADMIN.equals(principal.getRoleName());
        boolean isMember = memberRepository.existsByDepartmentIdAndUserIdAndActiveTrue(departmentId, principal.getId());
        if (!isAdmin && !isMember) {
            throw new ForbiddenException("You are not authorized to view members of this department");
        }
        return memberRepository.findByDepartmentIdAndActiveTrue(departmentId).stream()
                .map(DepartmentDtos.DepartmentMemberResponse::from)
                .toList();
    }
}

