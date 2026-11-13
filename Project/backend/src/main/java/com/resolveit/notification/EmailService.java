package com.resolveit.notification;

import com.resolveit.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties appProperties;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, AppProperties appProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.appProperties = appProperties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEmailNotification(EmailNotificationEvent event) {
        if (!appProperties.mail().enabled()) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("Email delivery enabled but JavaMailSender bean is unavailable");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(appProperties.mail().from());
            msg.setTo(event.recipientEmail());
            msg.setSubject(event.subject());
            msg.setText(event.body());
            sender.send(msg);
        } catch (Exception ex) {
            // Failure to send email must never undo a committed complaint update (Section 5.8).
            log.warn("Failed to deliver email notification to {}: {}", event.recipientEmail(), ex.getMessage());
        }
    }
}

