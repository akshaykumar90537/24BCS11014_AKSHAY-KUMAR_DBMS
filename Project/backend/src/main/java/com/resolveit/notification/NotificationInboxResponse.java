package com.resolveit.notification;

import com.resolveit.common.PageResponse;

public record NotificationInboxResponse(
        long unreadCount,
        PageResponse<NotificationResponse> page
) {}

