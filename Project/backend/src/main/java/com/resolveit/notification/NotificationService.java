package com.resolveit.notification;

import com.resolveit.common.Constants;
import com.resolveit.common.PageResponse;
import com.resolveit.department.DepartmentMemberRepository;
import com.resolveit.entity.Complaint;
import com.resolveit.entity.Notification;
import com.resolveit.entity.User;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.user.UserRepository;
import com.resolveit.workflow.MemberType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               DepartmentMemberRepository departmentMemberRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.departmentMemberRepository = departmentMemberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void notifyUser(User recipient, Complaint complaint, String type, String message, boolean sendEmail) {
        if (recipient == null || !recipient.isActive()) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setComplaint(complaint);
        notification.setType(type);
        notification.setMessage(message);
        notificationRepository.save(notification);

        if (sendEmail) {
            String subject = complaint != null
                    ? "[ResolveIt] Update on " + complaint.getComplaintNumber()
                    : "[ResolveIt] Notification";
            eventPublisher.publishEvent(new EmailNotificationEvent(recipient.getEmail(), subject, message));
        }
    }

    @Transactional
    public void notifyAdmins(Complaint complaint, String type, String message, boolean sendEmail) {
        List<User> admins = userRepository.findByRoleNameAndActiveTrue(Constants.ROLE_ADMIN);
        for (User admin : admins) {
            notifyUser(admin, complaint, type, message, sendEmail);
        }
    }

    @Transactional
    public void notifyDepartmentManagers(Long departmentId, Complaint complaint, String type, String message, boolean sendEmail) {
        if (departmentId == null) {
            return;
        }
        departmentMemberRepository
                .findByDepartmentIdAndMemberTypeAndActiveTrue(departmentId, MemberType.MANAGER)
                .forEach(dm -> notifyUser(dm.getUser(), complaint, type, message, sendEmail));
    }

    @Transactional
    public void notifyDistinctUsers(List<User> recipients, Complaint complaint, String type, String message, boolean sendEmail) {
        Set<Long> seen = new LinkedHashSet<>();
        for (User u : recipients) {
            if (u != null && seen.add(u.getId())) {
                notifyUser(u, complaint, type, message, sendEmail);
            }
        }
    }

    @Transactional(readOnly = true)
    public NotificationInboxResponse getInbox(Long userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
        long unread = notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
        return new NotificationInboxResponse(unread, PageResponse.from(page));
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllReadByRecipientId(userId, Instant.now());
    }
}

