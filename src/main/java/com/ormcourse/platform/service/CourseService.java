package com.ormcourse.platform.service;

import static com.ormcourse.platform.repository.CourseSpecifications.hasCategory;
import static com.ormcourse.platform.repository.CourseSpecifications.hasId;
import static com.ormcourse.platform.repository.CourseSpecifications.hasStatus;
import static com.ormcourse.platform.repository.CourseSpecifications.hasTag;
import static com.ormcourse.platform.repository.CourseSpecifications.hasTeacher;
import static com.ormcourse.platform.repository.CourseSpecifications.titleContains;

import com.ormcourse.platform.dto.common.PageResponse;
import com.ormcourse.platform.dto.course.CourseCreateRequest;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.course.CourseSearchFilter;
import com.ormcourse.platform.dto.course.CourseSummaryResponse;
import com.ormcourse.platform.dto.course.CourseUpdateRequest;
import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseModule;
import com.ormcourse.platform.entity.Tag;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.CourseModuleRepository;
import com.ormcourse.platform.repository.CourseRepository;
import com.ormcourse.platform.repository.EnrollmentRepository;
import com.ormcourse.platform.repository.TagRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final TagRepository tagRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CategoryService categoryService;
    private final UserLookup userLookup;

    @Transactional
    public CourseDetailsResponse create(CourseCreateRequest request) {
        Course course = new Course(
                request.title().trim(),
                request.description(),
                categoryService.find(request.categoryId()),
                userLookup.getTeacher(request.teacherId())
        );
        course.setDurationHours(request.durationHours());
        course.setStartDate(request.startDate());

        if (request.status() != null) {
            course.setStatus(request.status());
        }

        loadTags(request.tagIdsOrEmpty()).forEach(course::addTag);
        List<ModuleRequest> modules = request.modulesOrEmpty();

        for (int i = 0; i < modules.size(); i++) {
            course.addModule(CourseStructureFactory.buildModule(modules.get(i), i + 1));
        }

        courseRepository.save(course);
        log.info("Created course id={} title='{}' modules={}", course.getId(), course.getTitle(), modules.size());
        return CourseDetailsResponse.from(course, course.getModules());
    }

    public CourseDetailsResponse getDetails(Long id) {
        Course course = courseRepository.findWithDetailsById(id).orElseThrow(() -> NotFoundException.of("Course", id));
        List<CourseModule> modules = moduleRepository.findStructureByCourseId(id);
        return CourseDetailsResponse.from(course, modules);
    }

    public PageResponse<CourseSummaryResponse> search(CourseSearchFilter filter, Specification<Course> visibility, Pageable pageable) {
        Specification<Course> specification = Specification.allOf(
                visibility,
                hasCategory(filter.categoryId()),
                hasTeacher(filter.teacherId()),
                hasStatus(filter.status()),
                hasTag(filter.tag()),
                titleContains(filter.query())
        );
        return PageResponse.from(courseRepository.findAll(specification, pageable), CourseSummaryResponse::from);
    }

    public void requireVisible(Long id, Specification<Course> visibility) {
        if (!courseRepository.exists(Specification.allOf(hasId(id), visibility))) {
            throw NotFoundException.of("Course", id);
        }
    }

    @Transactional
    public CourseDetailsResponse update(Long id, CourseUpdateRequest request) {
        Course course = find(id);
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setDurationHours(request.durationHours());
        course.setStartDate(request.startDate());
        course.setStatus(request.status());

        if (!course.getCategory().getId().equals(request.categoryId())) {
            course.setCategory(categoryService.find(request.categoryId()));
        }

        if (!course.getTeacher().getId().equals(request.teacherId())) {
            course.setTeacher(userLookup.getTeacher(request.teacherId()));
        }

        courseRepository.flush();
        log.info("Updated course id={}", id);
        return getDetails(id);
    }

    @Transactional
    public void delete(Long id) {
        Course course = find(id);

        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ConflictException("Course " + id + " has enrolled students; archive it instead of deleting");
        }

        courseRepository.delete(course);
        log.info("Deleted course id={} with its modules, lessons, assignments and quizzes", id);
    }

    @Transactional
    public CourseSummaryResponse addTag(Long courseId, Long tagId) {
        Course course = courseRepository.findWithDetailsById(courseId).orElseThrow(() -> NotFoundException.of("Course", courseId));
        course.addTag(tagRepository.findById(tagId).orElseThrow(() -> NotFoundException.of("Tag", tagId)));
        return CourseSummaryResponse.from(course);
    }

    @Transactional
    public CourseSummaryResponse removeTag(Long courseId, Long tagId) {
        Course course = courseRepository.findWithDetailsById(courseId).orElseThrow(() -> NotFoundException.of("Course", courseId));
        Tag tag = course.getTags().stream()
                .filter(t -> t.getId().equals(tagId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Tag " + tagId + " is not assigned to course " + courseId));
        course.removeTag(tag);
        return CourseSummaryResponse.from(course);
    }

    Course find(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> NotFoundException.of("Course", id));
    }

    private List<Tag> loadTags(Set<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = tagRepository.findAllById(tagIds);

        if (tags.size() != tagIds.size()) {
            Set<Long> found = tags.stream().map(Tag::getId).collect(Collectors.toSet());
            List<Long> missing = tagIds.stream().filter(tagId -> !found.contains(tagId)).sorted().toList();
            throw new NotFoundException("Tags not found: " + missing);
        }

        return tags;
    }
}
