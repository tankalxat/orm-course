package com.ormcourse.platform.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.ormcourse.platform.entity.NotificationType;
import com.ormcourse.platform.event.NotificationCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    private static final NotificationCreatedEvent EVENT = new NotificationCreatedEvent(
            "anna@test.io",
            "Анна",
            NotificationType.SUBMISSION_GRADED,
            "Работа «JPA» оценена на 90 из 100"
    );

    @Mock
    private JavaMailSender mailSender;

    @Test
    void messageContainsRecipientSubjectAndGreeting() {
        EmailNotificationSender sender = sender();

        SimpleMailMessage message = sender.toMessage(EVENT);

        assertThat(message.getFrom()).isEqualTo("robot@test.io");
        assertThat(message.getTo()).containsExactly("anna@test.io");
        assertThat(message.getSubject()).isEqualTo("[Learning Platform] Работа оценена");
        assertThat(message.getText()).startsWith("Здравствуйте, Анна!").contains("оценена на 90 из 100");
    }

    @Test
    void sendsMessageAndSwallowsMailFailures() {
        EmailNotificationSender sender = sender();
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> sender.on(EVENT)).doesNotThrowAnyException();
        verify(mailSender).send(sender.toMessage(EVENT));
    }

    private EmailNotificationSender sender() {
        return new EmailNotificationSender(mailSender, new MailNotificationProperties(true, "robot@test.io"));
    }
}
