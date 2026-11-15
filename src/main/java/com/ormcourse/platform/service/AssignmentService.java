package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.assignment.AssignmentRequest;
import com.ormcourse.platform.dto.assignment.AssignmentResponse;
import com.ormcourse.platform.dto.assignment.StudentAssignmentResponse;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.entity.Assignment;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.Lesson;
import com.ormcourse.platform.entity.Submission;
import com.ormcourse.platform.event.AssignmentCreatedEvent;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.AssignmentRepository;
import com.ormcourse.platform.repository.SubmissionRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonService lessonService;
    private final UserLookup userLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AssignmentResponse create(Long lessonId, AssignmentRequest request) {
        Lesson lesson = lessonService.find(lessonId);
        Assignment assignment = new Assignment(request.title(), request.description(), request.dueDate(), request.maxScore());
        lesson.addAssignment(assignment);
        assignmentRepository.save(assignment);
        Course course = lesson.getModule().getCourse();
        eventPublisher.publishEvent(new AssignmentCreatedEvent(course.getId(), course.getTitle(), assignment.getTitle()));
        log.info("Created assignment id={} for lesson id={}", assignment.getId(), lessonId);
        return AssignmentResponse.from(assignment);
    }

    public AssignmentResponse get(Long id) {
        return AssignmentResponse.from(find(id));
    }

    public List<AssignmentResponse> listByLesson(Long lessonId) {
        lessonService.find(lessonId);
        return assignmentRepository.findByLessonIdOrderByIdAsc(lessonId).stream().map(AssignmentResponse::from).toList();
    }

    public List<StudentAssignmentResponse> listForStudent(Long studentId) {
        userLookup.getStudent(studentId);
        Map<Long, Submission> submissions = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity()));
        return assignmentRepository.findForStudent(studentId).stream()
                .map(a -> toStudentAssignment(a, submissions.get(a.getId())))
                .toList();
    }

    @Transactional
    public AssignmentResponse update(Long id, AssignmentRequest request) {
        Assignment assignment = find(id);
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore());
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void delete(Long id) {
        Assignment assignment = find(id);
        assignment.getLesson().getAssignments().remove(assignment);
        log.info("Deleted assignment id={} with its submissions", id);
    }

    Assignment find(Long id) {
        return assignmentRepository.findWithCourseById(id).orElseThrow(() -> NotFoundException.of("Assignment", id));
    }

    private static StudentAssignmentResponse toStudentAssignment(Assignment assignment, Submission submission) {
        Course course = assignment.getLesson().getModule().getCourse();
        return new StudentAssignmentResponse(
                course.getId(),
                course.getTitle(),
                AssignmentResponse.from(assignment),
                submission == null ? null : SubmissionResponse.from(submission)
        );
    }
}
