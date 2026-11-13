package com.resolveit.attachment;

import com.resolveit.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    long countByComplaintId(Long complaintId);

    @Query("""
            SELECT a FROM Attachment a
            JOIN FETCH a.uploader
            WHERE a.complaint.id = :complaintId
            ORDER BY a.createdAt ASC
            """)
    List<Attachment> findByComplaintIdWithUploader(@Param("complaintId") Long complaintId);
}

