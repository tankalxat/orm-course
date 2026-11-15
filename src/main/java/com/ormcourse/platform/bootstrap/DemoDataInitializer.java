package com.ormcourse.platform.bootstrap;

import com.ormcourse.platform.dto.assignment.AssignmentRequest;
import com.ormcourse.platform.dto.assignment.GradeRequest;
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
import com.ormcourse.platform.dto.quiz.TakeQuizRequest;
import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.dto.user.ProfileRequest;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.QuestionType;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.repository.UserRepository;
import com.ormcourse.platform.service.AssignmentService;
import com.ormcourse.platform.service.CategoryService;
import com.ormcourse.platform.service.CourseService;
import com.ormcourse.platform.service.EnrollmentService;
import com.ormcourse.platform.service.LessonService;
import com.ormcourse.platform.service.QuizAttemptService;
import com.ormcourse.platform.service.QuizService;
import com.ormcourse.platform.service.ReviewService;
import com.ormcourse.platform.service.SubmissionService;
import com.ormcourse.platform.service.TagService;
import com.ormcourse.platform.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private static final Duration TWO_WEEKS = Duration.ofDays(14);
    private static final Duration ONE_DAY = Duration.ofDays(1);

    private final UserRepository userRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final CourseService courseService;
    private final LessonService lessonService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;
    private final ReviewService reviewService;
    private final Clock clock;
    private final DemoDataProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo data skipped: database already contains users");
            return;
        }

        Users users = createUsers();
        Catalog catalog = createCatalog();
        CourseDetailsResponse hibernate = createHibernateCourse(users, catalog);
        CourseDetailsResponse sql = createSqlCourse(users, catalog);
        createDraftCourse(users, catalog);
        Content content = createContent(hibernate, sql);
        simulateLearning(users, hibernate, sql, content);
        log.info("Demo data loaded: courses '{}' and '{}'", hibernate.course().title(), sql.course().title());
    }

    private Users createUsers() {
        createUser("Администратор", "admin@learning.local", Role.ADMIN, null);
        Long hibernateTeacher = createUser("Ирина Смирнова", "irina.smirnova@learning.local", Role.TEACHER,
                new ProfileRequest("Java-разработчик, 10 лет с Hibernate", null, "Москва", "https://hibernate.org"));
        Long sqlTeacher = createUser("Павел Орлов", "pavel.orlov@learning.local", Role.TEACHER,
                new ProfileRequest("DBA и автор курсов по PostgreSQL", null, "Санкт-Петербург", null));
        Long anna = createUser("Анна Кузнецова", "anna@learning.local", Role.STUDENT,
                new ProfileRequest("Студентка, изучаю backend", null, "Казань", null));
        Long boris = createUser("Борис Иванов", "boris@learning.local", Role.STUDENT,
                new ProfileRequest("Переход из QA в разработку", null, "Новосибирск", null));
        Long vera = createUser("Вера Петрова", "vera@learning.local", Role.STUDENT, null);
        return new Users(hibernateTeacher, sqlTeacher, anna, boris, vera);
    }

    private Long createUser(String name, String email, Role role, ProfileRequest profile) {
        Long id = userService.create(new UserCreateRequest(name, email, properties.password(), role, null)).id();

        if (profile != null) {
            userService.upsertProfile(id, profile);
        }

        return id;
    }

    private Catalog createCatalog() {
        Long programming = categoryService.create(new CategoryRequest("Программирование", "Языки и фреймворки")).id();
        Long databases = categoryService.create(new CategoryRequest("Базы данных", "SQL, СУБД, моделирование")).id();
        Map<String, Long> tags = new HashMap<>();

        for (String name : List.of("java", "hibernate", "orm", "sql", "postgresql", "spring")) {
            tags.put(name, tagService.create(new TagRequest(name)).id());
        }

        return new Catalog(programming, databases, tags);
    }

    private CourseDetailsResponse createHibernateCourse(Users users, Catalog catalog) {
        ModuleRequest intro = new ModuleRequest("Введение в ORM", "Зачем нужен ORM и как устроена JPA", null, List.of(
                new LessonRequest("Что такое ORM", "Объектно-реляционное отображение, JPA и Hibernate.",
                        "https://example.com/videos/orm-intro", null, 25),
                new LessonRequest("Сущности и маппинг", "@Entity, @Id, @Column, генерация идентификаторов.",
                        "https://example.com/videos/entity-mapping", null, 40)
        ));
        ModuleRequest relations = new ModuleRequest("Связи и загрузка", "Ассоциации, каскады, стратегии загрузки", null, List.of(
                new LessonRequest("Связи между сущностями", "@OneToOne, @OneToMany, @ManyToMany, владелец связи.",
                        "https://example.com/videos/associations", null, 45),
                new LessonRequest("Lazy и Eager загрузка", "LazyInitializationException, JOIN FETCH, EntityGraph, N+1.",
                        "https://example.com/videos/fetching", null, 50)
        ));
        return courseService.create(new CourseCreateRequest(
                "Основы Hibernate",
                "Практический курс по JPA и Hibernate: от первой сущности до оптимизации запросов.",
                catalog.programming(),
                users.hibernateTeacher(),
                24,
                LocalDate.now(clock),
                CourseStatus.PUBLISHED,
                catalog.tagIds("java", "hibernate", "orm"),
                List.of(intro, relations)
        ));
    }

    private CourseDetailsResponse createSqlCourse(Users users, Catalog catalog) {
        ModuleRequest basics = new ModuleRequest("Запросы и соединения", "SELECT, JOIN, агрегаты", null, List.of(
                new LessonRequest("SELECT и фильтрация", "WHERE, ORDER BY, LIMIT.", null, null, 30),
                new LessonRequest("JOIN и агрегатные функции", "INNER/LEFT JOIN, GROUP BY, HAVING.", null, null, 45)
        ));
        return courseService.create(new CourseCreateRequest(
                "SQL для разработчиков",
                "Пишем эффективные запросы к PostgreSQL.",
                catalog.databases(),
                users.sqlTeacher(),
                16,
                LocalDate.now(clock),
                CourseStatus.PUBLISHED,
                catalog.tagIds("sql", "postgresql"),
                List.of(basics)
        ));
    }

    private void createDraftCourse(Users users, Catalog catalog) {
        courseService.create(new CourseCreateRequest(
                "Spring Data JPA: продвинутый уровень",
                "Specifications, проекции, аудит. Курс в разработке.",
                catalog.programming(),
                users.hibernateTeacher(),
                20,
                null,
                CourseStatus.DRAFT,
                catalog.tagIds("java", "spring", "orm"),
                null
        ));
    }

    private Content createContent(CourseDetailsResponse hibernate, CourseDetailsResponse sql) {
        Instant now = clock.instant();
        Long entityLessonId = hibernate.modules().getFirst().lessons().get(1).id();
        Long fetchLessonId = hibernate.modules().get(1).lessons().get(1).id();
        Long joinLessonId = sql.modules().getFirst().lessons().get(1).id();
        Long mappingTask = assignmentService.create(entityLessonId, new AssignmentRequest(
                "Спроектировать сущности", "Опишите сущности Course, Lesson, Student с аннотациями JPA.",
                now.plus(TWO_WEEKS), 100
        )).id();
        Long lazyTask = assignmentService.create(fetchLessonId, new AssignmentRequest(
                "Воспроизвести LazyInitializationException", "Покажите ошибку и два способа её исправить.",
                now.minus(ONE_DAY), 50
        )).id();
        Long sqlTask = assignmentService.create(joinLessonId, new AssignmentRequest(
                "Отчёт по продажам", "Напишите запрос с JOIN и GROUP BY по таблицам orders и customers.",
                now.plus(TWO_WEEKS), 10
        )).id();
        QuizResponse ormQuiz = quizService.create(hibernate.modules().getFirst().id(), ormQuiz());
        QuizResponse fetchQuiz = quizService.create(hibernate.modules().get(1).id(), fetchQuiz());
        QuizResponse sqlQuiz = quizService.create(sql.modules().getFirst().id(), sqlQuiz());
        return new Content(mappingTask, lazyTask, sqlTask, ormQuiz, fetchQuiz, sqlQuiz);
    }

    private QuizRequest ormQuiz() {
        return new QuizRequest("Тест: основы ORM", 10, 60, 3, List.of(
                single("Какая аннотация помечает класс как сущность JPA?", "@Entity", "@Table", "@Component"),
                multiple("Что относится к реализациям JPA?", List.of("Hibernate", "EclipseLink"), List.of("Lombok")),
                single("Что делает @GeneratedValue?", "Генерирует значение первичного ключа", "Создаёт таблицу", "Создаёт индекс")
        ));
    }

    private QuizRequest fetchQuiz() {
        return new QuizRequest("Тест: связи и загрузка", 15, 70, 2, List.of(
                single("Тип загрузки @ManyToOne по умолчанию?", "EAGER", "LAZY"),
                single("Когда возникает LazyInitializationException?",
                        "При обращении к LAZY-связи после закрытия сессии", "При каждом SELECT", "При сохранении сущности"),
                multiple("Как решить проблему N+1?", List.of("JOIN FETCH", "@EntityGraph", "@BatchSize"), List.of("FetchType.EAGER везде"))
        ));
    }

    private QuizRequest sqlQuiz() {
        return new QuizRequest("Тест: SQL-запросы", 10, 50, 3, List.of(
                single("Какой JOIN вернёт строки левой таблицы без пары?", "LEFT JOIN", "INNER JOIN", "CROSS JOIN"),
                single("Где фильтруют агрегированные значения?", "HAVING", "WHERE", "ORDER BY")
        ));
    }

    private static QuestionRequest single(String text, String correct, String... wrong) {
        return multipleOrSingle(text, QuestionType.SINGLE_CHOICE, List.of(correct), List.of(wrong));
    }

    private static QuestionRequest multiple(String text, List<String> correct, List<String> wrong) {
        return multipleOrSingle(text, QuestionType.MULTIPLE_CHOICE, correct, wrong);
    }

    private static QuestionRequest multipleOrSingle(String text, QuestionType type, List<String> correct, List<String> wrong) {
        List<AnswerOptionRequest> options = new ArrayList<>();
        correct.forEach(option -> options.add(new AnswerOptionRequest(option, true)));
        wrong.forEach(option -> options.add(new AnswerOptionRequest(option, false)));
        return new QuestionRequest(text, type, options);
    }

    private void simulateLearning(Users users, CourseDetailsResponse hibernate, CourseDetailsResponse sql, Content content) {
        Long hibernateId = hibernate.course().id();
        Long sqlId = sql.course().id();
        List.of(users.anna(), users.boris(), users.vera()).forEach(s -> enrollmentService.enroll(hibernateId, s));
        List.of(users.anna(), users.vera()).forEach(s -> enrollmentService.enroll(sqlId, s));
        completeLessons(users.anna(), hibernate, 4);
        completeLessons(users.boris(), hibernate, 2);
        completeLessons(users.vera(), hibernate, 1);
        completeLessons(users.anna(), sql, 2);

        submitAndGrade(content.mappingTask(), users.anna(), "Course 1-M CourseModule 1-M Lesson, Student M-M Course.", 95,
                "Отличная модель, учтены владельцы связей.");
        submitAndGrade(content.mappingTask(), users.boris(), "Сущности описаны, нет связей ManyToMany.", 70,
                "Добавьте связь студентов и курсов через Enrollment.");
        submitAndGrade(content.lazyTask(), users.anna(), "Исправление через JOIN FETCH и @EntityGraph.", 48,
                "Хорошо, но пример с DTO-проекцией тоже подошёл бы.");
        submitAndGrade(content.sqlTask(), users.vera(), "SELECT c.name, SUM(o.total) FROM orders o JOIN customers c ...", 9, null);
        submissionService.submit(content.mappingTask(), new SubmitRequest(users.vera(), "Черновик модели: Course, Lesson."));

        takeQuiz(content.ormQuiz(), users.anna(), true);
        takeQuiz(content.ormQuiz(), users.boris(), false);
        takeQuiz(content.ormQuiz(), users.boris(), true);
        takeQuiz(content.fetchQuiz(), users.anna(), true);
        takeQuiz(content.sqlQuiz(), users.vera(), true);

        reviewService.add(hibernateId, new ReviewRequest(users.anna(), 5, "Лучший курс по Hibernate, много практики."));
        reviewService.add(hibernateId, new ReviewRequest(users.boris(), 4, "Понятно, но хотелось бы больше видео."));
        reviewService.add(sqlId, new ReviewRequest(users.vera(), 5, "Наконец разобралась с JOIN."));
    }

    private void completeLessons(Long studentId, CourseDetailsResponse course, int count) {
        course.modules().stream()
                .flatMap(module -> module.lessons().stream())
                .limit(count)
                .forEach(lesson -> lessonService.markCompleted(lesson.id(), studentId));
    }

    private void submitAndGrade(Long assignmentId, Long studentId, String content, int score, String feedback) {
        Long submissionId = submissionService.submit(assignmentId, new SubmitRequest(studentId, content)).id();
        submissionService.grade(submissionId, new GradeRequest(score, feedback));
    }

    private void takeQuiz(QuizResponse quiz, Long studentId, boolean allCorrect) {
        Map<Long, Set<Long>> answers = new HashMap<>();

        for (QuestionResponse question : quiz.questions()) {
            answers.put(question.id(), pickOptions(question, allCorrect || answers.isEmpty()));
        }

        quizAttemptService.takeQuiz(quiz.id(), new TakeQuizRequest(studentId, answers));
    }

    private static Set<Long> pickOptions(QuestionResponse question, boolean correct) {
        Set<Long> picked = question.options().stream()
                .filter(option -> Boolean.TRUE.equals(option.correct()) == correct)
                .map(AnswerOptionResponse::id)
                .collect(Collectors.toSet());
        return question.type() == QuestionType.SINGLE_CHOICE ? Set.of(picked.iterator().next()) : picked;
    }

    private record Users(Long hibernateTeacher, Long sqlTeacher, Long anna, Long boris, Long vera) {
    }

    private record Catalog(Long programming, Long databases, Map<String, Long> tags) {

        Set<Long> tagIds(String... names) {
            return Arrays.stream(names).map(tags::get).collect(Collectors.toSet());
        }
    }

    private record Content(Long mappingTask, Long lazyTask, Long sqlTask, QuizResponse ormQuiz, QuizResponse fetchQuiz,
                           QuizResponse sqlQuiz) {
    }
}
