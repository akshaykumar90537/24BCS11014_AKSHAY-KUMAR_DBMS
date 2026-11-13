package com.resolveit.comment;

import com.resolveit.entity.Comment;
import com.resolveit.workflow.CommentVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.author
            WHERE c.complaint.id = :complaintId
              AND (:includeInternal = true OR c.visibility = :publicVisibility)
            ORDER BY c.createdAt ASC, c.id ASC
            """)
    List<Comment> findVisibleComments(
            @Param("complaintId") Long complaintId,
            @Param("includeInternal") boolean includeInternal,
            @Param("publicVisibility") CommentVisibility publicVisibility
    );
}

