package com.resolveit.comment;

import com.resolveit.entity.Comment;
import com.resolveit.workflow.CommentVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class CommentDtos {
    private CommentDtos() {}

    public record CreateCommentRequest(
            @NotBlank @Size(max = 5000) String body,
            CommentVisibility visibility
    ) {}

    public record CommentResponse(
            Long id,
            Long complaintId,
            Long authorId,
            String authorName,
            String authorRole,
            String body,
            CommentVisibility visibility,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static CommentResponse from(Comment c) {
            return new CommentResponse(
                    c.getId(),
                    c.getComplaint().getId(),
                    c.getAuthor().getId(),
                    c.getAuthor().getName(),
                    c.getAuthor().getRole().getName(),
                    c.getBody(),
                    c.getVisibility(),
                    c.getCreatedAt(),
                    c.getUpdatedAt()
            );
        }
    }
}

