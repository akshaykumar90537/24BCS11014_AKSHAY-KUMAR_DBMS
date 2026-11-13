package com.resolveit.workflow;

import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintHistory;
import com.resolveit.entity.User;
import com.resolveit.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class WorkflowService {

    private final TransitionPolicy transitionPolicy;
    private final ComplaintHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public WorkflowService(TransitionPolicy transitionPolicy,
                           ComplaintHistoryRepository historyRepository,
                           NotificationService notificationService) {
        this.transitionPolicy = transitionPolicy;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void recordEvent(Complaint complaint,
                            User actor,
                            EventType eventType,
                            ComplaintStatus oldStatus,
                            ComplaintStatus newStatus,
                            String reason,
                            Map<String, Object> metadata) {
        ComplaintHistory history = new ComplaintHistory();
        history.setComplaint(complaint);
        history.setActor(actor);
        history.setEventType(eventType);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setMetadata(metadata);
        historyRepository.save(history);
    }

    @Transactional
    public ComplaintStatus transitionStatus(Complaint complaint,
                                            ComplaintStatus targetStatus,
                                            User actor,
                                            EventType eventType,
                                            String reason,
                                            Map<String, Object> metadata) {
        ComplaintStatus previous = complaint.getStatus();
        String actorRole = actor != null ? actor.getRole().getName() : null;
        transitionPolicy.validateTransition(previous, targetStatus, actorRole);

        complaint.setStatus(targetStatus);
        Instant now = Instant.now();
        if (targetStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(now);
        } else if (targetStatus == ComplaintStatus.CLOSED) {
            complaint.setClosedAt(now);
        } else if (targetStatus == ComplaintStatus.REOPENED) {
            complaint.setResolvedAt(null);
            complaint.setClosedAt(null);
        }

        recordEvent(complaint, actor, eventType, previous, targetStatus, reason, metadata);

        notificationService.notifyUser(
                complaint.getCreator(),
                complaint,
                "STATUS_CHANGED",
                "Complaint " + complaint.getComplaintNumber() + " status changed from "
                        + previous + " to " + targetStatus + ".",
                true
        );

        return previous;
    }
}

