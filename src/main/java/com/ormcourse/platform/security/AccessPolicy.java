package com.ormcourse.platform.security;

import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.repository.AssignmentRepository;
import com.ormcourse.platform.repository.CourseModuleRepository;
import com.ormcourse.platform.repository.CourseRepository;
import com.ormcourse.platform.repository.CourseReviewRepository;
import com.ormcourse.platform.repository.CourseSpecifications;
import com.ormcourse.platform.repository.EnrollmentRepository;
import com.ormcourse.platform.repository.LessonRepository;
import com.ormcourse.platform.repository.NotificationRepository;
import com.ormcourse.platform.repository.QuestionRepository;
import com.ormcourse.platform.repository.QuizRepository;
import com.ormcourse.platform.repository.SubmissionRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("access")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("unused")
public class AccessPolicy {

    private static final String ROLE_PREFIX = "ROLE_";

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    public boolean isSelf(Authentication authentication, Long userId) {
        return userId != null && String.valueOf(userId).equals(authentication.getName());
    }

    public boolean isSelfOrAdmin(Authentication authentication, Long userId) {
        return isAdmin(authentication) || isSelf(authentication, userId);
    }

    public boolean isSelfOrStaff(Authentication authentication, Long userId) {
        return isSelfOrAdmin(authentication, userId) || hasRole(authentication, Role.TEACHER);
    }

    public boolean canAssignTeacher(Authentication authentication, Long teacherId) {
        return isAdmin(authentication) || hasRole(authentication, Role.TEACHER) && isSelf(authentication, teacherId);
    }

    public boolean canManageCourse(Authentication authentication, Long courseId) {
        return teaches(authentication, courseRepository.findTeacherIdById(courseId));
    }

    public boolean canManageModule(Authentication authentication, Long moduleId) {
        return teaches(authentication, moduleRepository.findTeacherIdById(moduleId));
    }

    public boolean canManageLesson(Authentication authentication, Long lessonId) {
        return teaches(authentication, lessonRepository.findTeacherIdById(lessonId));
    }

    public boolean canManageAssignment(Authentication authentication, Long assignmentId) {
        return teaches(authentication, assignmentRepository.findTeacherIdById(assignmentId));
    }

    public boolean canManageQuiz(Authentication authentication, Long quizId) {
        return teaches(authentication, quizRepository.findTeacherIdById(quizId));
    }

    public boolean canManageQuestion(Authentication authentication, Long questionId) {
        return teaches(authentication, questionRepository.findTeacherIdById(questionId));
    }

    public boolean canManageEnrollment(Authentication authentication, Long enrollmentId) {
        return teaches(authentication, enrollmentRepository.findTeacherIdById(enrollmentId));
    }

    public boolean canGradeSubmission(Authentication authentication, Long submissionId) {
        return teaches(authentication, submissionRepository.findTeacherIdById(submissionId));
    }

    public boolean canViewSubmission(Authentication authentication, Long submissionId) {
        return canGradeSubmission(authentication, submissionId) || ownsSubmission(authentication, submissionId);
    }

    public boolean ownsSubmission(Authentication authentication, Long submissionId) {
        return owns(authentication, submissionRepository.findStudentIdById(submissionId));
    }

    public boolean ownsReview(Authentication authentication, Long reviewId) {
        return isAdmin(authentication) || owns(authentication, reviewRepository.findStudentIdById(reviewId));
    }

    public boolean ownsNotification(Authentication authentication, Long notificationId) {
        return owns(authentication, notificationRepository.findRecipientIdById(notificationId));
    }

    public Specification<Course> visibleCourses(Authentication authentication) {
        if (isAdmin(authentication)) {
            return CourseSpecifications.anyCourse();
        }

        Long teacherId = hasRole(authentication, Role.TEACHER) ? Long.valueOf(authentication.getName()) : null;
        return CourseSpecifications.visibleTo(teacherId);
    }

    private boolean teaches(Authentication authentication, Optional<Long> teacherId) {
        return isAdmin(authentication) || owns(authentication, teacherId);
    }

    private boolean owns(Authentication authentication, Optional<Long> ownerId) {
        return ownerId.map(id -> isSelf(authentication, id)).orElse(true);
    }

    private static boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, Role.ADMIN);
    }

    private static boolean hasRole(Authentication authentication, Role role) {
        String authority = ROLE_PREFIX + role.name();
        return authentication.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
