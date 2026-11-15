package com.ormcourse.platform.dto.notification;

import com.ormcourse.platform.entity.Notification;
import com.ormcourse.platform.entity.NotificationType;
import java.time.Instant;

public record NotificationResponse(Long id, NotificationType type, String message, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
