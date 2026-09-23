package com.nfcplatform.notification.controller;

import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.response.ApiResponse;
import com.nfcplatform.notification.dto.NotificationResponse;
import com.nfcplatform.notification.service.NotificationService;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Scoped to the authenticated caller regardless of role (admin or client) - notifications
 * are addressed to a user, not a tenant, so there is no separate admin/client split here
 * unlike most other modules.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal actor,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(notificationService.listForUser(actor.getId(), pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(actor.getId())));
    }

    @PostMapping("/{uuid}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable String uuid,
                                                        @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(notificationService.markRead(uuid, actor.getId()));
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@AuthenticationPrincipal UserPrincipal actor) {
        notificationService.markAllRead(actor.getId());
        return ApiResponse.ok(null, "All notifications marked as read");
    }
}
