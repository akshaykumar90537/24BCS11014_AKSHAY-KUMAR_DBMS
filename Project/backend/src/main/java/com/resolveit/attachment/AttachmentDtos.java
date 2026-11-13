package com.resolveit.attachment;

import com.resolveit.entity.Attachment;

import java.time.Instant;

public final class AttachmentDtos {
    private AttachmentDtos() {}

    public record AttachmentResponse(
            Long id,
            Long complaintId,
            Long uploaderId,
            String uploaderName,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksum,
            Instant createdAt
    ) {
        public static AttachmentResponse from(Attachment a) {
            return new AttachmentResponse(
                    a.getId(),
                    a.getComplaint().getId(),
                    a.getUploader().getId(),
                    a.getUploader().getName(),
                    a.getOriginalFilename(),
                    a.getContentType(),
                    a.getSizeBytes(),
                    a.getChecksum(),
                    a.getCreatedAt()
            );
        }
    }
}

