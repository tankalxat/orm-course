package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.notification.NotificationResponse;
import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.dto.review.ReviewResponse;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewAndNotificationIntegrationTest extends IntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private NotificationService notificationService;

    @Test
    void onlyEnrolledStudentCanReviewCourseOnce() {
        Long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();

        assertThatThrownBy(() -> reviewService.add(courseId, new ReviewRequest(student, 5, null)))
                .isInstanceOf(BusinessRuleException.class);

        fixtures.enroll(courseId, student);
        ReviewResponse review = reviewService.add(courseId, new ReviewRequest(student, 5, "Nice"));

        assertThat(review.rating()).isEqualTo(5);
        assertThatThrownBy(() -> reviewService.add(courseId, new ReviewRequest(student, 3, null)))
                .isInstanceOf(ConflictException.class);

        reviewService.delete(review.id());

        assertThat(reviewService.list(courseId)).isEmpty();
    }

    @Test
    void notificationsCanBeFilteredAndMarkedRead() {
        Long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();
        fixtures.enroll(courseId, student);
        Long notificationId = notificationService.listForUser(student, true).get(0).id();

        NotificationResponse read = notificationService.markRead(notificationId);

        assertThat(read.read()).isTrue();
        assertThat(notificationService.listForUser(student, true)).isEmpty();
        List<NotificationResponse> all = notificationService.listForUser(student, false);
        assertThat(all).hasSize(1);
    }
}
