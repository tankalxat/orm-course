package com.ormcourse.platform.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.ormcourse.platform.dto.assignment.GradeRequest;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.service.SubmissionService;
import com.ormcourse.platform.service.UserService;
import com.ormcourse.platform.support.IntegrationTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "app.mail.enabled=true",
        "app.mail.from=robot@learning.test",
        "spring.mail.host=localhost",
        "spring.mail.port=3025"
})
class EmailNotificationIntegrationTest extends IntegrationTest {

    private static final long TIMEOUT_MS = 5_000;

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private UserService userService;

    @Autowired
    private SubmissionService submissionService;

    @Test
    void studentReceivesEmailsOnEnrollmentNewAssignmentAndGrade() throws Exception {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long student = fixtures.student();
        String email = userService.getById(student).email();

        fixtures.enroll(course.course().id(), student);
        Long submission = fixtures.submit(fixtures.assignment(course, null).id(), student).id();
        submissionService.grade(submission, new GradeRequest(90, "Great"));

        assertThat(GREEN_MAIL.waitForIncomingEmail(TIMEOUT_MS, 3)).isTrue();
        MimeMessage[] messages = GREEN_MAIL.getReceivedMessages();
        List<String> subjects = Arrays.stream(messages).map(EmailNotificationIntegrationTest::subject).toList();

        assertThat(subjects).containsExactlyInAnyOrder(
                "[Learning Platform] Запись на курс",
                "[Learning Platform] Новое задание",
                "[Learning Platform] Работа оценена"
        );
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(email);
            assertThat(message.getFrom()[0].toString()).isEqualTo("robot@learning.test");
        });
    }

    @Test
    void enrollmentRolledBackByBusinessRuleSendsNothing() throws Exception {
        Long student = fixtures.student();
        Long draft = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 1, 1).course().id();

        assertThatThrownBy(() -> fixtures.enroll(draft, student)).isInstanceOf(BusinessRuleException.class);

        assertThat(GREEN_MAIL.waitForIncomingEmail(1_000, 1)).isFalse();
    }

    private static String subject(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (MessagingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
