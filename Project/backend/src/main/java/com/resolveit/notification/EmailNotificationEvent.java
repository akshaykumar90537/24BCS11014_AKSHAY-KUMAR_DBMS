package com.resolveit.notification;

public record EmailNotificationEvent(
        String recipientEmail,
        String subject,
        String body
) {}

