package com.ormcourse.platform.event;

import com.ormcourse.platform.entity.NotificationType;

public record NotificationCreatedEvent(String recipientEmail, String recipientName, NotificationType type, String message) {
}
