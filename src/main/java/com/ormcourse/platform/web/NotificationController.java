package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.notification.NotificationResponse;
import com.ormcourse.platform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Уведомления пользователей")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/users/{userId}/notifications")
    @Operation(summary = "Уведомления пользователя")
    @PreAuthorize("@access.isSelfOrAdmin(authentication, #userId)")
    public List<NotificationResponse> list(@PathVariable Long userId, @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.listForUser(userId, unreadOnly);
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Отметить уведомление прочитанным")
    @PreAuthorize("@access.ownsNotification(authentication, #id)")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }
}
