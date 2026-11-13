package com.resolveit.assignment;

import com.resolveit.entity.ComplaintAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ComplaintAssignmentRepository extends JpaRepository<ComplaintAssignment, Long> {

    Optional<ComplaintAssignment> findByComplaintIdAndActiveTrue(Long complaintId);

    boolean existsByComplaintIdAndEmployeeIdAndActiveTrue(Long complaintId, Long employeeId);

    List<ComplaintAssignment> findByComplaintIdOrderByAssignedAtDesc(Long complaintId);

    @Query("""
            SELECT ca FROM ComplaintAssignment ca
            JOIN FETCH ca.complaint c
            LEFT JOIN FETCH c.department
            LEFT JOIN FETCH c.category
            WHERE ca.employee.id = :employeeId
              AND ca.active = true
            ORDER BY c.dueAt ASC NULLS LAST, c.createdAt DESC
            """)
    Page<ComplaintAssignment> findActiveAssignmentsByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    @Query("""
            SELECT ca.employee.id, ca.employee.name, ca.employee.email, COUNT(ca)
            FROM ComplaintAssignment ca
            JOIN ca.complaint c
            WHERE ca.active = true
              AND c.department.id = :departmentId
              AND c.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
            GROUP BY ca.employee.id, ca.employee.name, ca.employee.email
            """)
    List<Object[]> countActiveWorkloadByDepartment(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("""
            UPDATE ComplaintAssignment ca
               SET ca.active = false,
                   ca.unassignedAt = :now
             WHERE ca.complaint.id = :complaintId
               AND ca.active = true
            """)
    int deactivateActiveByComplaintId(@Param("complaintId") Long complaintId, @Param("now") Instant now);
}

