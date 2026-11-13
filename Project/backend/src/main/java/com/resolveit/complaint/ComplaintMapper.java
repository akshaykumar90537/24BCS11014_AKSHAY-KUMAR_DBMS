package com.resolveit.complaint;

import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintAssignment;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public ComplaintDtos.ComplaintResponse toResponse(Complaint c, ComplaintAssignment activeAssignment) {
        return new ComplaintDtos.ComplaintResponse(
                c.getId(),
                c.getComplaintNumber(),
                c.getTitle(),
                c.getDescription(),
                c.getStatus(),
                c.getPriority(),
                c.getCategory() != null ? c.getCategory().getId() : null,
                c.getCategory() != null ? c.getCategory().getName() : null,
                c.getDepartment() != null ? c.getDepartment().getId() : null,
                c.getDepartment() != null ? c.getDepartment().getName() : null,
                c.getCreator() != null ? c.getCreator().getId() : null,
                c.getCreator() != null ? c.getCreator().getName() : null,
                activeAssignment != null ? activeAssignment.getEmployee().getId() : null,
                activeAssignment != null ? activeAssignment.getEmployee().getName() : null,
                activeAssignment != null ? activeAssignment.getId() : null,
                c.getLocation(),
                c.getDueAt(),
                c.getResolvedAt(),
                c.getClosedAt(),
                c.isEscalated(),
                c.getEscalatedAt(),
                c.getResolutionNotes(),
                c.getVersion(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}

