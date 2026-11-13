package com.resolveit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveit.category.CategoryRepository;
import com.resolveit.common.Constants;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.department.DepartmentRepository;
import com.resolveit.entity.*;
import com.resolveit.escalation.EscalationService;
import com.resolveit.notification.NotificationRepository;
import com.resolveit.user.RoleRepository;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.MemberType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplaintAcceptanceIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired DepartmentMemberRepository departmentMemberRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ComplaintRepository complaintRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired EscalationService escalationService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactionTemplate;

    private Category category;
    private Department deptA;
    private Department deptB;
    private User admin;
    private User managerA;
    private User employeeA;
    private User employeeB;
    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS complaint_number_seq START WITH 1 INCREMENT BY 1").executeUpdate();
        });

        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(Constants.ROLE_USER));
            roleRepository.save(new Role(Constants.ROLE_ADMIN));
            roleRepository.save(new Role(Constants.ROLE_MANAGER));
            roleRepository.save(new Role(Constants.ROLE_EMPLOYEE));
        }

        if (userRepository.count() == 0) {
            deptA = createDepartment("Facilities");
            deptB = createDepartment("IT Services");

            Category cat = new Category();
            cat.setName("Plumbing");
            cat.setDefaultDepartment(deptA);
            cat.setActive(true);
            category = categoryRepository.save(cat);

            admin = createUser("Admin", "admin@test.local", Constants.ROLE_ADMIN);
            managerA = createUser("Manager A", "manager.a@test.local", Constants.ROLE_MANAGER);
            employeeA = createUser("Employee A", "emp.a@test.local", Constants.ROLE_EMPLOYEE);
            employeeB = createUser("Employee B", "emp.b@test.local", Constants.ROLE_EMPLOYEE);
            userA = createUser("User A", "user.a@test.local", Constants.ROLE_USER);
            userB = createUser("User B", "user.b@test.local", Constants.ROLE_USER);

            addMember(deptA, managerA, MemberType.MANAGER);
            addMember(deptA, employeeA, MemberType.EMPLOYEE);
            addMember(deptB, employeeB, MemberType.EMPLOYEE);
        } else {
            category = categoryRepository.findAll().get(0);
            deptA = departmentRepository.findByNameIgnoreCase("Facilities").orElseThrow();
            deptB = departmentRepository.findByNameIgnoreCase("IT Services").orElseThrow();
            admin = userRepository.findByEmailIgnoreCase("admin@test.local").orElseThrow();
            managerA = userRepository.findByEmailIgnoreCase("manager.a@test.local").orElseThrow();
            employeeA = userRepository.findByEmailIgnoreCase("emp.a@test.local").orElseThrow();
            employeeB = userRepository.findByEmailIgnoreCase("emp.b@test.local").orElseThrow();
            userA = userRepository.findByEmailIgnoreCase("user.a@test.local").orElseThrow();
            userB = userRepository.findByEmailIgnoreCase("user.b@test.local").orElseThrow();
        }
    }

    @Test
    void registerLoginAndNeverLeakPasswords() throws Exception {
        String registerPayload = """
                {"name":"New Citizen","email":"citizen.new@test.local","password":"Password123!"}
                """;
        MvcResult regRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andReturn();

        assertFalse(regRes.getResponse().getContentAsString().contains("passwordHash"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"citizen.new@test.local\",\"password\":\"WrongPass!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enforcesOwnershipDepartmentAndWorkflowAndFeedbackAndIdempotentEscalation() throws Exception {
        String userAToken = login("user.a@test.local");
        String userBToken = login("user.b@test.local");
        String adminToken = login("admin@test.local");
        String mgrAToken = login("manager.a@test.local");
        String empAToken = login("emp.a@test.local");
        String empBToken = login("emp.b@test.local");

        // User A creates complaint
        MvcResult createRes = mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Broken pipe","description":"Water leaking near stairwell","categoryId":%d,"priority":"HIGH","location":"Block A"}
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.complaintNumber").value(startsWith("CMP-")))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn();

        JsonNode createdData = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data");
        long complaintId = createdData.get("id").asLong();

        // User B cannot read User A's complaint
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        // Invalid status transition returns 409 Conflict
        mockMvc.perform(post("/api/complaints/" + complaintId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\"Done\"}"))
                .andExpect(status().isConflict());

        // Admin routes to deptA
        mockMvc.perform(post("/api/admin/complaints/" + complaintId + "/route")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":%d}".formatted(deptA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        // Manager A cannot assign Employee B (outside deptA)
        mockMvc.perform(post("/api/complaints/" + complaintId + "/assignments")
                        .header("Authorization", "Bearer " + mgrAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":%d,\"reason\":\"Trying outside dept\"}".formatted(employeeB.getId())))
                .andExpect(status().isForbidden());

        // Manager A assigns Employee A
        mockMvc.perform(post("/api/complaints/" + complaintId + "/assignments")
                        .header("Authorization", "Bearer " + mgrAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":%d,\"reason\":\"Assigning plumbing specialist\"}".formatted(employeeA.getId())))
                .andExpect(status().isCreated());

        // Employee B cannot access complaint assigned to Employee A
        mockMvc.perform(get("/api/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + empBToken))
                .andExpect(status().isForbidden());

        // Employee A adds internal note; User A cannot add internal note and cannot read it
        mockMvc.perform(post("/api/complaints/" + complaintId + "/comments")
                        .header("Authorization", "Bearer " + empAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Internal pipe inspection note\",\"visibility\":\"INTERNAL\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/complaints/" + complaintId + "/comments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Trying internal\",\"visibility\":\"INTERNAL\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/complaints/" + complaintId + "/comments")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Attachment upload rejects disallowed file type (.exe)
        MockMultipartFile exeFile = new MockMultipartFile("file", "bad.exe", "application/octet-stream", "MZ".getBytes());
        mockMvc.perform(multipart("/api/complaints/" + complaintId + "/attachments")
                        .file(exeFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isUnsupportedMediaType());

        // Idempotent escalation test: move due_at to the past and run scheduler twice
        transactionTemplate.executeWithoutResult(st -> {
            Complaint c = complaintRepository.findById(complaintId).orElseThrow();
            c.setDueAt(Instant.now().minus(2, ChronoUnit.HOURS));
            complaintRepository.save(c);
        });
        assertEquals(1, escalationService.escalateOverdueComplaints());
        assertEquals(0, escalationService.escalateOverdueComplaints());

        // Employee A starts and resolves complaint
        mockMvc.perform(post("/api/complaints/" + complaintId + "/start")
                        .header("Authorization", "Bearer " + empAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/complaints/" + complaintId + "/resolve")
                        .header("Authorization", "Bearer " + empAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNotes\":\"Replaced faulty valve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // Feedback validation & duplicate blocking
        mockMvc.perform(post("/api/complaints/" + complaintId + "/feedback")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"comment\":\"Too high\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/complaints/" + complaintId + "/feedback")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Great service\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/complaints/" + complaintId + "/feedback")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Duplicate\"}"))
                .andExpect(status().isConflict());
    }

    private String login(String email) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private Department createDepartment(String name) {
        Department d = new Department();
        d.setName(name);
        d.setActive(true);
        return departmentRepository.save(d);
    }

    private User createUser(String name, String email, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Password123!"));
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

