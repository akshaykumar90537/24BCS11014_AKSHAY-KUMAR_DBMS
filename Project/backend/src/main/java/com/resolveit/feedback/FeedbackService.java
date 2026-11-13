package com.resolveit.feedback;

import com.resolveit.complaint.ComplaintAccessService;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.Feedback;
import com.resolveit.entity.User;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.ComplaintStatus;
import com.resolveit.workflow.EventType;
import com.resolveit.workflow.WorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintAccessService accessService;
    private final WorkflowService workflowService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ComplaintRepository complaintRepository,
                           UserRepository userRepository,
                           ComplaintAccessService accessService,
                           WorkflowService workflowService) {
        this.feedbackRepository = feedbackRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.workflowService = workflowService;
    }

    @Transactional
    public FeedbackDtos.FeedbackResponse submitFeedback(Long complaintId,
                                                        UserPrincipal principal,
                                                        FeedbackDtos.SubmitFeedbackRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getCreator().getId().equals(principal.getId())) {
            throw new ForbiddenException("Only the complainant can submit feedback for this complaint");
        }

        if (complaint.getStatus() != ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new ConflictException("FEEDBACK_NOT_ALLOWED", "Feedback can only be submitted after resolution or closure");
        }

        if (feedbackRepository.existsByComplaintId(complaintId)) {
            throw new ConflictException("FEEDBACK_ALREADY_SUBMITTED", "Feedback has already been submitted for this complaint");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Feedback feedback = new Feedback();
        feedback.setComplaint(complaint);
        feedback.setUser(user);
        feedback.setRating((short) request.rating());
        feedback.setComment(request.comment() != null ? request.comment().trim() : null);
        feedback = feedbackRepository.save(feedback);

        workflowService.recordEvent(
                complaint,
                user,
                EventType.FEEDBACK_SUBMITTED,
                complaint.getStatus(),
                complaint.getStatus(),
                "Rating " + request.rating() + "/5 submitted",
                Map.of("rating", request.rating())
        );

        return FeedbackDtos.FeedbackResponse.from(feedback);
    }

    @Transactional(readOnly = true)
    public FeedbackDtos.FeedbackResponse getFeedback(Long complaintId, UserPrincipal principal) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireCanView(complaint, principal);
        return feedbackRepository.findByComplaintId(complaintId)
                .map(FeedbackDtos.FeedbackResponse::from)
                .orElse(null);
    }
}

