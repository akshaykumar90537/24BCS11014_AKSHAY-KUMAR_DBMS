package com.resolveit.feedback;

import com.resolveit.entity.Feedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    public record SubmitFeedbackRequest(
            @Min(1) @Max(5) int rating,
            @Size(max = 2000) String comment
    ) {}

    public record FeedbackResponse(
            Long id,
            Long complaintId,
            Long userId,
            String userName,
            int rating,
            String comment,
            Instant createdAt
    ) {
        public static FeedbackResponse from(Feedback f) {
            return new FeedbackResponse(
                    f.getId(),
                    f.getComplaint().getId(),
                    f.getUser().getId(),
                    f.getUser().getName(),
                    f.getRating(),
                    f.getComment(),
                    f.getCreatedAt()
            );
        }
    }
}

