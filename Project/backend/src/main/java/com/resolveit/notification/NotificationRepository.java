package com.resolveit.notification;

import com.resolveit.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            LEFT JOIN FETCH n.complaint
            WHERE n.recipient.id = :recipientId
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(@Param("recipientId") Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipient.id = :recipientId AND n.readAt IS NULL")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId, @Param("now") Instant now);
}

