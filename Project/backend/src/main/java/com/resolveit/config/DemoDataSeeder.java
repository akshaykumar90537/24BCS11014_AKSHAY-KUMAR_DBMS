package com.resolveit.config;

import com.resolveit.common.Constants;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.department.DepartmentRepository;
import com.resolveit.entity.Department;
import com.resolveit.entity.DepartmentMember;
import com.resolveit.entity.Role;
import com.resolveit.entity.User;
import com.resolveit.user.RoleRepository;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.MemberType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = getRole(Constants.ROLE_ADMIN);
        Role managerRole = getRole(Constants.ROLE_MANAGER);
        Role employeeRole = getRole(Constants.ROLE_EMPLOYEE);
        Role userRole = getRole(Constants.ROLE_USER);

        createUser("System Admin", "admin@resolveit.local", "Admin@1234", adminRole);
        User citizen = createUser("Aarav Sharma", "user@resolveit.local", "User@1234", userRole);
        User citizen2 = createUser("Priya Verma", "priya@resolveit.local", "User@1234", userRole);

        Department facilities = departmentRepository.findByNameIgnoreCase("Facilities & Maintenance").orElse(null);
        Department itDept = departmentRepository.findByNameIgnoreCase("IT & Network Services").orElse(null);

        User facilitiesMgr = createUser("Rohan Mehta", "manager@resolveit.local", "Manager@1234", managerRole);
        User itMgr = createUser("Neha Kapoor", "it.manager@resolveit.local", "Manager@1234", managerRole);

        User emp1 = createUser("Vikram Singh", "employee@resolveit.local", "Employee@1234", employeeRole);
        User emp2 = createUser("Sneha Nair", "sneha.staff@resolveit.local", "Employee@1234", employeeRole);
        User emp3 = createUser("Karan Patel", "it.staff@resolveit.local", "Employee@1234", employeeRole);

        if (facilities != null) {
            addMember(facilities, facilitiesMgr, MemberType.MANAGER);
            addMember(facilities, emp1, MemberType.EMPLOYEE);
            addMember(facilities, emp2, MemberType.EMPLOYEE);
        }
        if (itDept != null) {
            addMember(itDept, itMgr, MemberType.MANAGER);
            addMember(itDept, emp3, MemberType.EMPLOYEE);
        }

        log.info("Seeded default ResolveIt demo users (admin@resolveit.local, manager@resolveit.local, employee@resolveit.local, user@resolveit.local) with citizen ids {}, {}",
                citizen.getId(), citizen2.getId());
    }

    private Role getRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private User createUser(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setActive(true);
        return userRepository.save(u);
    }

    private void addMember(Department dept, User user, MemberType type) {
        DepartmentMember dm = new DepartmentMember();
        dm.setDepartment(dept);
        dm.setUser(user);
        dm.setMemberType(type);
        dm.setActive(true);
        departmentMemberRepository.save(dm);
    }
}

