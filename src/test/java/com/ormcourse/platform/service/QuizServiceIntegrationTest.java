package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.quiz.AnswerOptionRequest;
import com.ormcourse.platform.dto.quiz.QuestionRequest;
import com.ormcourse.platform.dto.quiz.QuestionResponse;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.dto.quiz.QuizSubmissionResponse;
import com.ormcourse.platform.dto.quiz.TakeQuizRequest;
import com.ormcourse.platform.entity.QuestionType;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.support.IntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class QuizServiceIntegrationTest extends IntegrationTest {

    private static final int MAX_ATTEMPTS = 2;
    private static final int CONCURRENT_ATTEMPTS = 6;

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizAttemptService quizAttemptService;

    private CourseDetailsResponse course;
    private Long student;
    private QuizResponse quiz;

    @BeforeEach
    void setUp() {
        course = fixtures.publishedCourse();
        student = fixtures.student();
        fixtures.enroll(course.course().id(), student);
        quiz = fixtures.quiz(fixtures.firstModuleId(course), MAX_ATTEMPTS);
    }

    @Test
    void quizIsStoredWithQuestionsAndOptions() {
        assertThat(countRows("questions")).isEqualTo(2);
        assertThat(countRows("answer_options")).isEqualTo(5);
        assertThat(quizService.getByModule(fixtures.firstModuleId(course), true).questions()).hasSize(2);
    }

    @Test
    void correctAnswersAreHiddenUnlessRequested() {
        QuizResponse forStudent = quizService.get(quiz.id(), false);

        assertThat(forStudent.questions()).flatExtracting(QuestionResponse::options).allSatisfy(o -> assertThat(o.correct()).isNull());
        assertThat(quizService.get(quiz.id(), true).questions()).flatExtracting(QuestionResponse::options)
                .anySatisfy(o -> assertThat(o.correct()).isTrue());
    }

    @Test
    void allCorrectAnswersGiveFullScoreAndStoreSelectedOptions() {
        QuizSubmissionResponse result = take(fixtures.answers(quiz, 2));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.percentage()).isEqualTo(100);
        assertThat(result.passed()).isTrue();
        assertThat(result.attemptNumber()).isEqualTo(1);
        assertThat(countRows("quiz_submission_answers")).isEqualTo(3);
    }

    @Test
    void partiallyCorrectAnswersBelowPassingScoreFail() {
        QuizSubmissionResponse result = take(fixtures.answers(quiz, 1));

        assertThat(result.percentage()).isEqualTo(50);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void attemptLimitIsEnforced() {
        take(fixtures.answers(quiz, 0));
        QuizSubmissionResponse second = take(fixtures.answers(quiz, 2));

        assertThat(second.attemptNumber()).isEqualTo(2);
        assertThatThrownBy(() -> take(fixtures.answers(quiz, 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Attempt limit");
        assertThat(quizAttemptService.listByQuiz(quiz.id())).hasSize(2);
        assertThat(quizAttemptService.listByStudent(student)).hasSize(2);
        assertThat(quizAttemptService.listByCourse(course.course().id())).hasSize(2);
    }

    @Test
    void concurrentAttemptsNeverExceedLimit() throws Exception {
        Map<Long, Set<Long>> answers = fixtures.answers(quiz, 2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS)) {
            List<Future<?>> attempts = IntStream.range(0, CONCURRENT_ATTEMPTS)
                    .<Future<?>>mapToObj(i -> executor.submit(() -> {
                        start.await();
                        return take(answers);
                    }))
                    .toList();
            start.countDown();

            for (Future<?> attempt : attempts) {
                try {
                    attempt.get();
                } catch (ExecutionException ex) {
                    assertThat(ex.getCause()).isInstanceOfAny(BusinessRuleException.class, DataIntegrityViolationException.class);
                }
            }
        }

        assertThat(quizAttemptService.listByStudent(student))
                .extracting(QuizSubmissionResponse::attemptNumber)
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .allSatisfy(number -> assertThat(number).isBetween(1, MAX_ATTEMPTS));
    }

    @Test
    void singleChoiceQuestionAcceptsOnlyOneAnswer() {
        QuestionResponse single = quiz.questions().get(0);
        Set<Long> both = Set.of(single.options().get(0).id(), single.options().get(1).id());

        assertThatThrownBy(() -> take(Map.of(single.id(), both)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only one answer");
    }

    @Test
    void optionsFromAnotherQuestionAreRejected() {
        QuestionResponse single = quiz.questions().get(0);
        Long foreignOption = quiz.questions().get(1).options().get(0).id();

        assertThatThrownBy(() -> take(Map.of(single.id(), Set.of(foreignOption))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not belong");
    }

    @Test
    void notEnrolledStudentCannotTakeQuiz() {
        Long stranger = fixtures.student();

        assertThatThrownBy(() -> quizAttemptService.takeQuiz(quiz.id(), new TakeQuizRequest(stranger, fixtures.answers(quiz, 2))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void questionsCanBeChangedOnlyBeforeFirstAttempt() {
        QuestionRequest extra = new QuestionRequest("Extra?", QuestionType.SINGLE_CHOICE, List.of(
                new AnswerOptionRequest("yes", true),
                new AnswerOptionRequest("no", false)
        ));
        QuestionResponse added = quizService.addQuestion(quiz.id(), extra);
        assertThat(added.orderIndex()).isEqualTo(3);
        quizService.deleteQuestion(added.id());

        take(fixtures.answers(quiz, 2));

        assertThatThrownBy(() -> quizService.addQuestion(quiz.id(), extra)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> quizService.deleteQuestion(quiz.questions().get(0).id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void moduleCanHaveOnlyOneQuiz() {
        assertThatThrownBy(() -> fixtures.quiz(fixtures.firstModuleId(course), 1)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deletingQuizRemovesQuestionsAndResults() {
        take(fixtures.answers(quiz, 2));

        quizService.delete(quiz.id());

        assertThat(countRows("quizzes")).isZero();
        assertThat(countRows("questions")).isZero();
        assertThat(countRows("quiz_submissions")).isZero();
        assertThat(countRows("quiz_submission_answers")).isZero();
        assertThatThrownBy(() -> quizService.getByModule(fixtures.firstModuleId(course), false))
                .isInstanceOf(NotFoundException.class);
    }

    private QuizSubmissionResponse take(Map<Long, Set<Long>> answers) {
        return quizAttemptService.takeQuiz(quiz.id(), new TakeQuizRequest(student, answers));
    }
}
