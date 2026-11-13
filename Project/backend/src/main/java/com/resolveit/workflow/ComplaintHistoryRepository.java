package com.resolveit.workflow;

import com.resolveit.entity.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    @Query("""
            SELECT h FROM ComplaintHistory h
            LEFT JOIN FETCH h.actor
            WHERE h.complaint.id = :complaintId
            ORDER BY h.createdAt ASC, h.id ASC
            """)
    List<ComplaintHistory> findByComplaintIdWithActor(@Param("complaintId") Long complaintId);
}

