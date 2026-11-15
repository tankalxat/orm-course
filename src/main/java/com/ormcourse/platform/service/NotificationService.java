package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.notification.NotificationResponse;
import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.entity.Notification;
import com.ormcourse.platform.entity.NotificationType;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.event.AssignmentCreatedEvent;
import com.ormcourse.platform.event.NotificationCreatedEvent;
import com.ormcourse.platform.event.QuizCompletedEvent;
import com.ormcourse.platform.event.StudentEnrolledEvent;
import com.ormcourse.platform.event.SubmissionGradedEvent;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.EnrollmentRepository;
import com.ormcourse.platform.repository.NotificationRepository;
import com.ormcourse.platform.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final UserLookup userLookup;
    private final ApplicationEventPublisher eventPublisher;

    public List<NotificationResponse> listForUser(Long userId, boolean unreadOnly) {
        userLookup.getUser(userId);
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> NotFoundException.of("Notification", notificationId));
        notification.setRead(true);
        return NotificationResponse.from(notification);
    }

    @EventListener
    @Transactional
    public void on(StudentEnrolledEvent event) {
        String message = "Вы записаны на курс «" + event.courseTitle() + "»";
        notify(userLookup.getUser(event.studentId()), NotificationType.ENROLLED, message);
    }

    @EventListener
    @Transactional
    public void on(AssignmentCreatedEvent event) {
        List<Long> students = enrollmentRepository.findStudentIdsByCourseIdAndStatus(event.courseId(), EnrollmentStatus.ACTIVE);
        String message = "Новое задание «" + event.assignmentTitle() + "» в курсе «" + event.courseTitle() + "»";
        List<User> recipients = userRepository.findAllById(students);
        recipients.forEach(student -> notify(student, NotificationType.NEW_ASSIGNMENT, message));
        log.debug("Sent {} notifications about assignment '{}'", recipients.size(), event.assignmentTitle());
    }

    @EventListener
    @Transactional
    public void on(SubmissionGradedEvent event) {
        String message = "Работа «" + event.assignmentTitle() + "» оценена: " + event.score() + "/" + event.maxScore();
        notify(userLookup.getUser(event.studentId()), NotificationType.SUBMISSION_GRADED, message);
    }

    @EventListener
    @Transactional
    public void on(QuizCompletedEvent event) {
        String verdict = event.passed() ? "пройден" : "не пройден";
        String message = "Тест «" + event.quizTitle() + "» " + verdict + ": " + event.percentage() + "%";
        notify(userLookup.getUser(event.studentId()), NotificationType.QUIZ_COMPLETED, message);
    }

    private void notify(User recipient, NotificationType type, String message) {
        notificationRepository.save(new Notification(recipient, type, message));
        eventPublisher.publishEvent(new NotificationCreatedEvent(recipient.getEmail(), recipient.getName(), type, message));
    }
}
