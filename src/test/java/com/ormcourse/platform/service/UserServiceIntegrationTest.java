package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.dto.user.ProfileRequest;
import com.ormcourse.platform.dto.user.ProfileResponse;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.dto.user.UserResponse;
import com.ormcourse.platform.dto.user.UserUpdateRequest;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.support.IntegrationTest;
import com.ormcourse.platform.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserLookup userLookup;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void emailIsNormalizedAndUniqueIgnoringCase() {
        UserCreateRequest ann = new UserCreateRequest(" Ann ", " Ann@Mail.Test ", TestFixtures.PASSWORD, Role.STUDENT, "+7 900");
        UserCreateRequest duplicate = new UserCreateRequest("Other", "ANN@mail.test", TestFixtures.PASSWORD, Role.TEACHER, null);

        UserResponse created = userService.create(ann);

        assertThat(created.email()).isEqualTo("ann@mail.test");
        assertThat(created.name()).isEqualTo("Ann");
        assertThatThrownBy(() -> userService.create(duplicate)).isInstanceOf(ConflictException.class);
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        Long id = fixtures.student();

        String hash = jdbcTemplate.queryForObject("select password_hash from users where id = ?", String.class, id);

        assertThat(hash).startsWith("$2").isNotEqualTo(TestFixtures.PASSWORD);
        assertThat(passwordEncoder.matches(TestFixtures.PASSWORD, hash)).isTrue();
    }

    @Test
    void updateRejectsEmailOfAnotherUser() {
        Long first = fixtures.student();
        Long second = fixtures.student();
        String takenEmail = userService.getById(first).email();

        assertThat(userService.update(second, new UserUpdateRequest("Renamed", "renamed@test.local", null)).name())
                .isEqualTo("Renamed");
        assertThatThrownBy(() -> userService.update(second, new UserUpdateRequest("X", takenEmail, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listFiltersByRole() {
        fixtures.student();
        fixtures.student();
        fixtures.teacher();

        assertThat(userService.list(Role.STUDENT, PageRequest.of(0, 10)).totalElements()).isEqualTo(2);
        assertThat(userService.list(null, PageRequest.of(0, 10)).totalElements()).isEqualTo(3);
    }

    @Test
    void profileSharesPrimaryKeyWithUserAndIsUpdatedInPlace() {
        Long user = fixtures.student();

        assertThatThrownBy(() -> userService.getProfile(user)).isInstanceOf(NotFoundException.class);

        userService.upsertProfile(user, new ProfileRequest("Bio", null, "Kazan", null));
        ProfileResponse updated = userService.upsertProfile(user, new ProfileRequest("New bio", null, "Moscow", null));

        assertThat(updated.city()).isEqualTo("Moscow");
        assertThat(userService.getProfile(user).bio()).isEqualTo("New bio");
        assertThat(countRows("profiles")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select user_id from profiles", Long.class)).isEqualTo(user);
    }

    @Test
    void deletingStudentRemovesProfileEnrollmentsWorkAndNotifications() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long student = fixtures.student();
        fixtures.enroll(course.course().id(), student);
        fixtures.submit(fixtures.assignment(course, null).id(), student);
        reviewService.add(course.course().id(), new ReviewRequest(student, 4, null));
        userService.upsertProfile(student, new ProfileRequest("Bio", null, null, null));

        userService.delete(student);

        assertThat(countRows("profiles")).isZero();
        assertThat(countRows("enrollments")).isZero();
        assertThat(countRows("submissions")).isZero();
        assertThat(countRows("course_reviews")).isZero();
        assertThat(countRows("notifications")).isZero();
        assertThat(countRows("courses")).isEqualTo(1);
    }

    @Test
    void teacherWithCoursesCannotBeDeleted() {
        Long teacher = fixtures.publishedCourse().course().teacherId();

        assertThatThrownBy(() -> userService.delete(teacher)).isInstanceOf(ConflictException.class);
    }

    @Test
    void roleSpecificLookupRejectsWrongRole() {
        Long teacher = fixtures.teacher();
        Long student = fixtures.student();

        assertThat(userLookup.getTeacher(teacher).getId()).isEqualTo(teacher);
        assertThatThrownBy(() -> userLookup.getTeacher(student)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> userLookup.getUser(-1L)).isInstanceOf(NotFoundException.class);
    }
}
