package com.resolveit.notification;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List current user's notifications with pagination and unread count")
    public ApiResponse<NotificationInboxResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ApiResponse.ok(notificationService.getInbox(principal.getId(), pageable));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Notification marked as read", notificationService.markAsRead(id, principal.getId()));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all current user's notifications as read")
    public ApiResponse<Map<String, Integer>> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        int updated = notificationService.markAllAsRead(principal.getId());
        return ApiResponse.ok("Notifications marked as read", Map.of("updatedCount", updated));
    }
}

