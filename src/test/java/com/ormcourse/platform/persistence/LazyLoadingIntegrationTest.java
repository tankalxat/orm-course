package com.ormcourse.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.course.CourseSearchFilter;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.repository.CourseRepository;
import com.ormcourse.platform.repository.CourseSpecifications;
import com.ormcourse.platform.service.CourseService;
import com.ormcourse.platform.support.IntegrationTest;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

class LazyLoadingIntegrationTest extends IntegrationTest {

    private static final int COURSES = 5;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void lazyCollectionAccessedOutsideTransactionThrowsLazyInitializationException() {
        Long courseId = fixtures.publishedCourse().course().id();

        Course course = courseRepository.findById(courseId).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getModules())).isFalse();
        assertThatThrownBy(() -> course.getModules().size()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void lazyManyToOneIsProxyThatFailsOutsideTransactionExceptForId() {
        CourseDetailsResponse details = fixtures.publishedCourse();

        Course course = courseRepository.findById(details.course().id()).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getTeacher())).isFalse();
        assertThat(course.getTeacher().getId()).isEqualTo(details.course().teacherId());
        assertThatThrownBy(() -> course.getTeacher().getName()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void joinFetchQueryInitializesCollectionBeforeSessionCloses() {
        Long courseId = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 3, 1).course().id();

        Course course = courseRepository.findWithModulesById(courseId).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getModules())).isTrue();
        assertThat(course.getModules()).hasSize(3);
    }

    @Test
    void entityGraphInitializesManyToOneAssociations() {
        CourseDetailsResponse details = fixtures.publishedCourse();

        Course course = courseRepository.findWithDetailsById(details.course().id()).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getTeacher())).isTrue();
        assertThat(Hibernate.isInitialized(course.getCategory())).isTrue();
        assertThat(course.getTeacher().getName()).isEqualTo(details.course().teacherName());
    }

    @Test
    void accessInsideTransactionLoadsLazyDataOnDemand() {
        Long courseId = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 2, 2).course().id();

        Integer lessons = transactionTemplate.execute(status -> courseRepository.findById(courseId).orElseThrow()
                .getModules().stream()
                .mapToInt(module -> module.getLessons().size())
                .sum()
        );

        assertThat(lessons).isEqualTo(4);
    }

    @Test
    void courseDetailsAreLoadedWithConstantNumberOfQueries() {
        Long courseId = fixtures.course(fixtures.teacher(), CourseStatus.PUBLISHED, 4, 3).course().id();
        statistics().clear();

        CourseDetailsResponse details = courseService.getDetails(courseId);

        assertThat(details.modules()).hasSize(4);
        assertThat(details.modules()).allSatisfy(module -> assertThat(module.lessons()).hasSize(3));
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void batchFetchingAvoidsNPlusOneWhenTouchingLazyProxies() {
        for (int i = 0; i < COURSES; i++) {
            fixtures.publishedCourse();
        }

        statistics().clear();

        List<String> teachers = transactionTemplate.execute(status -> courseRepository.findAll().stream()
                .map(course -> course.getTeacher().getName())
                .toList()
        );

        assertThat(teachers).hasSize(COURSES);
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    void courseSearchDoesNotTriggerQueryPerCourse() {
        for (int i = 0; i < COURSES; i++) {
            fixtures.publishedCourse();
        }

        statistics().clear();

        var page = courseService.search(
                new CourseSearchFilter(null, null, CourseStatus.PUBLISHED, null, null),
                CourseSpecifications.anyCourse(),
                PageRequest.of(0, 10)
        );

        assertThat(page.content()).hasSize(COURSES);
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }
}
