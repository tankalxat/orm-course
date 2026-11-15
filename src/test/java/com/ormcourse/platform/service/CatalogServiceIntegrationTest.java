package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.catalog.CategoryRequest;
import com.ormcourse.platform.dto.catalog.CategoryResponse;
import com.ormcourse.platform.dto.catalog.TagRequest;
import com.ormcourse.platform.dto.common.PageResponse;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.course.CourseSearchFilter;
import com.ormcourse.platform.dto.course.CourseSummaryResponse;
import com.ormcourse.platform.dto.course.CourseUpdateRequest;
import com.ormcourse.platform.dto.course.LessonRequest;
import com.ormcourse.platform.dto.course.LessonResponse;
import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.dto.course.ModuleResponse;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.CourseSpecifications;
import com.ormcourse.platform.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CatalogServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private LessonService lessonService;

    @Test
    void categoryNamesAreUniqueAndCategoryWithCoursesCannotBeDeleted() {
        CategoryResponse category = categoryService.create(new CategoryRequest("Java", "JVM"));
        Long courseCategory = fixtures.publishedCourse().course().categoryId();

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("java", null))).isInstanceOf(ConflictException.class);
        assertThat(categoryService.update(category.id(), new CategoryRequest("Kotlin", null)).name()).isEqualTo("Kotlin");
        assertThatThrownBy(() -> categoryService.delete(courseCategory)).isInstanceOf(ConflictException.class);

        categoryService.delete(category.id());

        assertThatThrownBy(() -> categoryService.get(category.id())).isInstanceOf(NotFoundException.class);
        assertThat(categoryService.list()).hasSize(1);
    }

    @Test
    void tagNamesAreUnique() {
        tagService.create(new TagRequest("jpa"));

        assertThatThrownBy(() -> tagService.create(new TagRequest("JPA"))).isInstanceOf(ConflictException.class);
        assertThat(tagService.list()).hasSize(1);
    }

    @Test
    void searchCombinesFiltersWithSpecifications() {
        CourseDetailsResponse hibernate = fixtures.publishedCourse();
        CourseDetailsResponse draft = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 1, 1);
        Long tag = fixtures.tag("hibernate");
        courseService.addTag(hibernate.course().id(), tag);

        assertThat(search(new CourseSearchFilter(null, null, CourseStatus.DRAFT, null, null)))
                .extracting(CourseSummaryResponse::id).containsExactly(draft.course().id());
        assertThat(search(new CourseSearchFilter(null, null, null, "HIBERNATE", null)))
                .extracting(CourseSummaryResponse::id).containsExactly(hibernate.course().id());
        assertThat(search(new CourseSearchFilter(draft.course().categoryId(), draft.course().teacherId(), null, null, "course")))
                .extracting(CourseSummaryResponse::id).containsExactly(draft.course().id());
        assertThat(search(new CourseSearchFilter(null, null, null, null, "missing"))).isEmpty();
    }

    @Test
    void courseUpdateChangesFieldsCategoryAndTeacher() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long newTeacher = fixtures.teacher();
        Long newCategory = fixtures.category();
        CourseUpdateRequest request = new CourseUpdateRequest(
                "Renamed", "Updated", newCategory, newTeacher, 42, LocalDate.of(2027, 1, 1), CourseStatus.ARCHIVED
        );

        CourseSummaryResponse updated = courseService.update(course.course().id(), request).course();

        assertThat(updated.title()).isEqualTo("Renamed");
        assertThat(updated.teacherId()).isEqualTo(newTeacher);
        assertThat(updated.categoryId()).isEqualTo(newCategory);
        assertThat(updated.status()).isEqualTo(CourseStatus.ARCHIVED);
        assertThat(updated.durationHours()).isEqualTo(42);
    }

    @Test
    void modulesAndLessonsGetNextOrderIndexAndCanBeEdited() {
        CourseDetailsResponse course = fixtures.publishedCourse();

        ModuleResponse module = moduleService.addModule(course.course().id(), new ModuleRequest("Second", null, null, null));
        LessonResponse lesson = lessonService.addLesson(module.id(), new LessonRequest("Intro", "Text", null, null, 15));
        LessonResponse updatedLesson = lessonService.update(lesson.id(), new LessonRequest("Intro v2", "Text", null, 5, 20));
        ModuleResponse updatedModule = moduleService.update(module.id(), new ModuleRequest("Second v2", "Desc", 7, null));

        assertThat(module.orderIndex()).isEqualTo(2);
        assertThat(lesson.orderIndex()).isEqualTo(1);
        assertThat(updatedLesson.orderIndex()).isEqualTo(5);
        assertThat(updatedModule.title()).isEqualTo("Second v2");
        assertThat(moduleService.get(module.id()).lessons()).hasSize(1);
        assertThat(lessonService.get(lesson.id()).courseId()).isEqualTo(course.course().id());

        lessonService.delete(lesson.id());

        assertThat(moduleService.get(module.id()).lessons()).isEmpty();
    }

    @Test
    void lessonCompletionIsIdempotent() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long student = fixtures.student();
        fixtures.enroll(course.course().id(), student);

        lessonService.markCompleted(fixtures.firstLessonId(course), student);
        lessonService.markCompleted(fixtures.firstLessonId(course), student);

        assertThat(countRows("lesson_completions")).isEqualTo(1);
    }

    private List<CourseSummaryResponse> search(CourseSearchFilter filter) {
        PageResponse<CourseSummaryResponse> page = courseService.search(filter, CourseSpecifications.anyCourse(), PageRequest.of(0, 10));
        return page.content();
    }
}
