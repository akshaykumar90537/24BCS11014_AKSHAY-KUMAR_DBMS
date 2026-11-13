package com.resolveit.feedback;

import com.resolveit.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByComplaintId(Long complaintId);

    Optional<Feedback> findByComplaintId(Long complaintId);

    @Query("SELECT AVG(f.rating) FROM Feedback f")
    Double findAverageRating();

    @Query("SELECT f.rating, COUNT(f) FROM Feedback f GROUP BY f.rating ORDER BY f.rating ASC")
    List<Object[]> countGroupedByRating();
}

