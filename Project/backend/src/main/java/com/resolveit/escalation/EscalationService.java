package com.resolveit.escalation;

import com.resolveit.assignment.ComplaintAssignmentRepository;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.config.AppProperties;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintAssignment;
import com.resolveit.entity.ComplaintHistory;
import com.resolveit.notification.NotificationService;
import com.resolveit.workflow.ComplaintHistoryRepository;
import com.resolveit.workflow.EventType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class EscalationService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final ComplaintAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    public EscalationService(ComplaintRepository complaintRepository,
                             ComplaintHistoryRepository historyRepository,
                             ComplaintAssignmentRepository assignmentRepository,
                             NotificationService notificationService,
                             AppProperties appProperties) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.appProperties = appProperties;
    }

    @Transactional
    public int escalateOverdueComplaints() {
        Instant now = Instant.now();
        int batchSize = Math.max(10, appProperties.sla().batchSize());
        List<Complaint> overdue = complaintRepository.findOverdueUnescalated(now, PageRequest.of(0, batchSize));
        int escalatedCount = 0;

        for (Complaint complaint : overdue) {
            // Idempotent atomic flag transition guarantees no duplicate escalation events/notifications.
            int updated = complaintRepository.markEscalatedAtomically(complaint.getId(), now);
            if (updated == 1) {
                escalatedCount++;
                ComplaintHistory history = new ComplaintHistory();
                history.setComplaint(complaint);
                history.setActor(null);
                history.setEventType(EventType.SLA_ESCALATED);
                history.setOldStatus(complaint.getStatus());
                history.setNewStatus(complaint.getStatus());
                history.setReason("SLA due time breached");
                history.setMetadata(Map.of(
                        "dueAt", complaint.getDueAt().toString(),
                        "escalatedAt", now.toString()
                ));
                historyRepository.save(history);

                String msg = "SLA breached for complaint " + complaint.getComplaintNumber()
                        + " (" + complaint.getPriority() + "). Immediate attention required.";

                notificationService.notifyAdmins(complaint, "SLA_ESCALATED", msg, true);
                if (complaint.getDepartment() != null) {
                    notificationService.notifyDepartmentManagers(
                            complaint.getDepartment().getId(), complaint, "SLA_ESCALATED", msg, true);
                }
                assignmentRepository.findByComplaintIdAndActiveTrue(complaint.getId())
                        .map(ComplaintAssignment::getEmployee)
                        .ifPresent(emp -> notificationService.notifyUser(emp, complaint, "SLA_ESCALATED", msg, true));
            }
        }
        return escalatedCount;
    }
}

