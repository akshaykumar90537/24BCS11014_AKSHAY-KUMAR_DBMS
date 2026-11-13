package com.resolveit.comment;

import com.resolveit.assignment.ComplaintAssignmentRepository;
import com.resolveit.complaint.ComplaintAccessService;
import com.resolveit.complaint.ComplaintRepository;
import com.resolveit.entity.Comment;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.ComplaintAssignment;
import com.resolveit.entity.User;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.notification.NotificationService;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.CommentVisibility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintAssignmentRepository assignmentRepository;
    private final ComplaintAccessService accessService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          ComplaintRepository complaintRepository,
                          UserRepository userRepository,
                          ComplaintAssignmentRepository assignmentRepository,
                          ComplaintAccessService accessService,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.accessService = accessService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentDtos.CommentResponse addComment(Long complaintId,
                                                  UserPrincipal principal,
                                                  CommentDtos.CreateCommentRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireCanView(complaint, principal);

        CommentVisibility visibility = request.visibility() != null
                ? request.visibility()
                : CommentVisibility.PUBLIC;

        if (visibility == CommentVisibility.INTERNAL && !accessService.canAddInternalComment(complaint, principal)) {
            throw new ForbiddenException("Only authorized staff can add internal notes");
        }

        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setComplaint(complaint);
        comment.setAuthor(author);
        comment.setBody(request.body().trim());
        comment.setVisibility(visibility);
        comment = commentRepository.save(comment);

        if (visibility == CommentVisibility.PUBLIC) {
            List<User> recipients = new ArrayList<>();
            if (!complaint.getCreator().getId().equals(author.getId())) {
                recipients.add(complaint.getCreator());
            }
            assignmentRepository.findByComplaintIdAndActiveTrue(complaintId)
                    .map(ComplaintAssignment::getEmployee)
                    .filter(emp -> !emp.getId().equals(author.getId()))
                    .ifPresent(recipients::add);

            notificationService.notifyDistinctUsers(
                    recipients,
                    complaint,
                    "PUBLIC_COMMENT",
                    author.getName() + " commented on complaint " + complaint.getComplaintNumber(),
                    false
            );
        }

        return CommentDtos.CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDtos.CommentResponse> listComments(Long complaintId, UserPrincipal principal) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        accessService.requireCanView(complaint, principal);

        boolean includeInternal = accessService.canViewInternalDetails(complaint, principal);
        return commentRepository.findVisibleComments(complaintId, includeInternal, CommentVisibility.PUBLIC)
                .stream()
                .map(CommentDtos.CommentResponse::from)
                .toList();
    }
}

