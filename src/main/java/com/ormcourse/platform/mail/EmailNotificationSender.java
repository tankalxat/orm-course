package com.ormcourse.platform.mail;

import com.ormcourse.platform.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class EmailNotificationSender {

    private static final String SUBJECT_PREFIX = "[Learning Platform] ";

    private final JavaMailSender mailSender;
    private final MailNotificationProperties properties;

    @Async
    @TransactionalEventListener
    public void on(NotificationCreatedEvent event) {
        try {
            mailSender.send(toMessage(event));
            log.info("Email '{}' sent to {}", event.type(), event.recipientEmail());
        } catch (MailException ex) {
            log.warn("Failed to send email '{}' to {}: {}", event.type(), event.recipientEmail(), ex.getMessage());
        }
    }

    SimpleMailMessage toMessage(NotificationCreatedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(event.recipientEmail());
        message.setSubject(SUBJECT_PREFIX + event.type().getSubject());
        message.setText("Здравствуйте, " + event.recipientName() + "!\n\n" + event.message() + "\n\n— Learning Platform");
        return message;
    }
}
