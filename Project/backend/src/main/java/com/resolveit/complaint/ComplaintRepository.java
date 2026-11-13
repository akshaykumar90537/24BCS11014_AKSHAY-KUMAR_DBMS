package com.resolveit.complaint;

import com.resolveit.entity.Complaint;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    Optional<Complaint> findByComplaintNumberIgnoreCase(String complaintNumber);

    @Query(value = "SELECT nextval('complaint_number_seq')", nativeQuery = true)
    Long nextComplaintSequence();

    @Query("""
            SELECT c FROM Complaint c
            WHERE c.category.id = :categoryId
              AND c.createdAt >= :since
              AND c.status NOT IN ('CLOSED', 'REJECTED')
            ORDER BY c.createdAt DESC
            """)
    List<Complaint> findRecentPotentialDuplicates(
            @Param("categoryId") Long categoryId,
            @Param("since") Instant since,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM Complaint c
            WHERE c.dueAt IS NOT NULL
              AND c.dueAt < :now
              AND c.escalated = false
              AND c.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
            ORDER BY c.dueAt ASC
            """)
    List<Complaint> findOverdueUnescalated(@Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Complaint c
               SET c.escalated = true,
                   c.escalatedAt = :now,
                   c.updatedAt = :now
             WHERE c.id = :id
               AND c.escalated = false
               AND c.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
            """)
    int markEscalatedAtomically(@Param("id") Long id, @Param("now") Instant now);

    @Query("""
            SELECT c FROM Complaint c
            WHERE c.department.id IN :departmentIds
              AND c.dueAt IS NOT NULL
              AND (c.dueAt < :now OR c.escalated = true)
              AND c.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
            ORDER BY c.dueAt ASC
            """)
    Page<Complaint> findOverdueByDepartmentIds(
            @Param("departmentIds") List<Long> departmentIds,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            SELECT c.status, COUNT(c) FROM Complaint c
            WHERE (:from IS NULL OR c.createdAt >= :from)
              AND (:to IS NULL OR c.createdAt <= :to)
              AND (:departmentId IS NULL OR c.department.id = :departmentId)
            GROUP BY c.status
            """)
    List<Object[]> countGroupedByStatus(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("departmentId") Long departmentId
    );

    @Query("""
            SELECT c.priority, COUNT(c) FROM Complaint c
            WHERE (:from IS NULL OR c.createdAt >= :from)
              AND (:to IS NULL OR c.createdAt <= :to)
              AND (:departmentId IS NULL OR c.department.id = :departmentId)
            GROUP BY c.priority
            """)
    List<Object[]> countGroupedByPriority(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("departmentId") Long departmentId
    );

    @Query("""
            SELECT c FROM Complaint c
            LEFT JOIN FETCH c.department
            LEFT JOIN FETCH c.category
            WHERE (:from IS NULL OR c.createdAt >= :from)
              AND (:to IS NULL OR c.createdAt <= :to)
              AND (:departmentId IS NULL OR c.department.id = :departmentId)
              AND (:categoryId IS NULL OR c.category.id = :categoryId)
              AND (:priority IS NULL OR c.priority = :priority)
              AND (:status IS NULL OR c.status = :status)
            ORDER BY c.createdAt DESC
            """)
    List<Complaint> findForReportFiltered(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("departmentId") Long departmentId,
            @Param("categoryId") Long categoryId,
            @Param("priority") Priority priority,
            @Param("status") ComplaintStatus status
    );

    long countByCreatorId(Long creatorId);
    long countByCreatorIdAndStatus(Long creatorId, ComplaintStatus status);
    long countByStatus(ComplaintStatus status);
    long countByEscalatedTrueAndStatusNotIn(List<ComplaintStatus> statuses);
}

