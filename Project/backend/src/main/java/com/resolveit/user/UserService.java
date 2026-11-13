package com.resolveit.user;

import com.resolveit.common.Constants;
import com.resolveit.common.PageResponse;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.department.DepartmentRepository;
import com.resolveit.entity.Department;
import com.resolveit.entity.DepartmentMember;
import com.resolveit.entity.Role;
import com.resolveit.entity.User;
import com.resolveit.exception.BadRequestException;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.workflow.MemberType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       DepartmentRepository departmentRepository,
                       DepartmentMemberRepository departmentMemberRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.departmentMemberRepository = departmentMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(String role, Boolean active, String search, Pageable pageable) {
        String normalizedRole = (role != null && !role.isBlank()) ? normalizeRole(role) : null;
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<UserDto> page = userRepository.searchUsers(normalizedRole, active, normalizedSearch, pageable)
                .map(this::toUserDtoWithDepartments);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserDtoWithDepartments(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("EMAIL_EXISTS", "An account with this email already exists");
        }
        String roleName = normalizeRole(request.role());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("INVALID_ROLE", "Invalid role: " + request.role()));

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);

        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            MemberType memberType = resolveMemberType(request.memberType(), roleName);
            DepartmentMember dm = new DepartmentMember();
            dm.setDepartment(department);
            dm.setUser(user);
            dm.setMemberType(memberType);
            dm.setActive(true);
            departmentMemberRepository.save(dm);
        }

        return toUserDtoWithDepartments(user);
    }

    @Transactional
    public UserDto updateRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String roleName = normalizeRole(request.role());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("INVALID_ROLE", "Invalid role: " + request.role()));
        user.setRole(role);
        return toUserDtoWithDepartments(userRepository.save(user));
    }

    @Transactional
    public UserDto updateStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(request.active());
        return toUserDtoWithDepartments(userRepository.save(user));
    }

    public UserDto toUserDtoWithDepartments(User user) {
        List<UserDto.UserDepartmentDto> depts = departmentMemberRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(dm -> new UserDto.UserDepartmentDto(
                        dm.getDepartment().getId(),
                        dm.getDepartment().getName(),
                        dm.getMemberType().name()
                ))
                .toList();
        return UserDto.from(user, depts);
    }

    private String normalizeRole(String role) {
        String trimmed = role.trim().toUpperCase();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    private MemberType resolveMemberType(String memberType, String roleName) {
        if (memberType != null && !memberType.isBlank()) {
            try {
                return MemberType.valueOf(memberType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("INVALID_MEMBER_TYPE", "Member type must be MANAGER or EMPLOYEE");
            }
        }
        return Constants.ROLE_MANAGER.equals(roleName) ? MemberType.MANAGER : MemberType.EMPLOYEE;
    }
}

