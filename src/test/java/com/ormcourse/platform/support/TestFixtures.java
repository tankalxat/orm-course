package com.ormcourse.platform.support;

import com.ormcourse.platform.dto.assignment.AssignmentRequest;
import com.ormcourse.platform.dto.assignment.AssignmentResponse;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.dto.assignment.SubmitRequest;
import com.ormcourse.platform.dto.catalog.CategoryRequest;
import com.ormcourse.platform.dto.catalog.TagRequest;
import com.ormcourse.platform.dto.course.CourseCreateRequest;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.course.LessonRequest;
import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.dto.quiz.AnswerOptionRequest;
import com.ormcourse.platform.dto.quiz.AnswerOptionResponse;
import com.ormcourse.platform.dto.quiz.QuestionRequest;
import com.ormcourse.platform.dto.quiz.QuestionResponse;
import com.ormcourse.platform.dto.quiz.QuizRequest;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.QuestionType;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.repository.UserRepository;
import com.ormcourse.platform.security.JwtTokenService;
import com.ormcourse.platform.service.AssignmentService;
import com.ormcourse.platform.service.CategoryService;
import com.ormcourse.platform.service.CourseService;
import com.ormcourse.platform.service.EnrollmentService;
import com.ormcourse.platform.service.QuizService;
import com.ormcourse.platform.service.SubmissionService;
import com.ormcourse.platform.service.TagService;
import com.ormcourse.platform.service.UserService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
@RequiredArgsConstructor
public class TestFixtures {

    public static final int MAX_SCORE = 100;
    public static final String PASSWORD = "password123";

    private final AtomicInteger sequence = new AtomicInteger();
    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;
    private final UserRepository userRepository;
    private final JwtTokenService tokenService;

    public Long teacher() {
        return user(Role.TEACHER);
    }

    public Long student() {
        return user(Role.STUDENT);
    }

    public Long user(Role role) {
        int number = sequence.incrementAndGet();
        String email = role.name().toLowerCase() + number + "@test.local";
        return userService.create(new UserCreateRequest(role.name() + " " + number, email, PASSWORD, role, null)).id();
    }

    public String bearer(Long userId) {
        return "Bearer " + tokenService.issue(userRepository.findById(userId).orElseThrow()).accessToken();
    }

    public Long category() {
        return categoryService.create(new CategoryRequest("Category " + sequence.incrementAndGet(), null)).id();
    }

    public Long tag(String name) {
        return tagService.create(new TagRequest(name)).id();
    }

    public CourseDetailsResponse publishedCourse() {
        return course(teacher(), CourseStatus.PUBLISHED, 1, 2);
    }

    public CourseDetailsResponse course(Long teacherId, CourseStatus status, int modules, int lessonsPerModule) {
        List<ModuleRequest> moduleRequests = IntStream.rangeClosed(1, modules)
                .mapToObj(m -> new ModuleRequest("Module " + m, null, null, lessons(lessonsPerModule)))
                .toList();
        return courseService.create(new CourseCreateRequest(
                "Course " + sequence.incrementAndGet(),
                "Description",
                category(),
                teacherId,
                10,
                null,
                status,
                Set.of(),
                moduleRequests
        ));
    }

    public Long firstModuleId(CourseDetailsResponse course) {
        return course.modules().get(0).id();
    }

    public Long firstLessonId(CourseDetailsResponse course) {
        return course.modules().get(0).lessons().get(0).id();
    }

    public AssignmentResponse assignment(CourseDetailsResponse course, Instant dueDate) {
        AssignmentRequest request = new AssignmentRequest("Assignment " + sequence.incrementAndGet(), null, dueDate, MAX_SCORE);
        return assignmentService.create(firstLessonId(course), request);
    }

    public SubmissionResponse submit(Long assignmentId, Long studentId) {
        return submissionService.submit(assignmentId, new SubmitRequest(studentId, "Solution"));
    }

    public void enroll(Long courseId, Long studentId) {
        enrollmentService.enroll(courseId, studentId);
    }

    public QuizResponse quiz(Long moduleId, int maxAttempts) {
        QuestionRequest single = new QuestionRequest("Single?", QuestionType.SINGLE_CHOICE, List.of(
                new AnswerOptionRequest("right", true),
                new AnswerOptionRequest("wrong", false)
        ));
        QuestionRequest multiple = new QuestionRequest("Multiple?", QuestionType.MULTIPLE_CHOICE, List.of(
                new AnswerOptionRequest("right one", true),
                new AnswerOptionRequest("right two", true),
                new AnswerOptionRequest("wrong", false)
        ));
        return quizService.create(moduleId, new QuizRequest("Quiz", 10, 60, maxAttempts, List.of(single, multiple)));
    }

    public Map<Long, Set<Long>> answers(QuizResponse quiz, int correctQuestions) {
        Map<Long, Set<Long>> answers = new HashMap<>();
        List<QuestionResponse> questions = quiz.questions();

        for (int i = 0; i < questions.size(); i++) {
            answers.put(questions.get(i).id(), options(questions.get(i), i < correctQuestions));
        }

        return answers;
    }

    private static Set<Long> options(QuestionResponse question, boolean correct) {
        Set<Long> ids = question.options().stream()
                .filter(option -> Boolean.TRUE.equals(option.correct()) == correct)
                .map(AnswerOptionResponse::id)
                .collect(Collectors.toSet());
        return question.type() == QuestionType.SINGLE_CHOICE ? Set.of(ids.iterator().next()) : ids;
    }

    private static List<LessonRequest> lessons(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(l -> new LessonRequest("Lesson " + l, "Content", null, null, 30))
                .toList();
    }
}
