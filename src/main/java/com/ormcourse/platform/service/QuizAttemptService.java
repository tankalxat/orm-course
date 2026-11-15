package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.quiz.QuizSubmissionResponse;
import com.ormcourse.platform.dto.quiz.TakeQuizRequest;
import com.ormcourse.platform.entity.Question;
import com.ormcourse.platform.entity.Quiz;
import com.ormcourse.platform.entity.QuizResult;
import com.ormcourse.platform.entity.QuizSubmission;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.event.QuizCompletedEvent;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.repository.QuestionRepository;
import com.ormcourse.platform.repository.QuizSubmissionRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizAttemptService {

    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuestionRepository questionRepository;
    private final QuizService quizService;
    private final CourseService courseService;
    private final UserLookup userLookup;
    private final EnrollmentGuard enrollmentGuard;
    private final QuizGrader quizGrader;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public QuizSubmissionResponse takeQuiz(Long quizId, TakeQuizRequest request) {
        Quiz quiz = quizService.find(quizId);
        User student = userLookup.getStudent(request.studentId());
        enrollmentGuard.requireActiveEnrollment(student.getId(), quiz.getModule().getCourse().getId());
        int attempts = quizSubmissionRepository.countByQuizIdAndStudentId(quizId, student.getId());

        if (attempts >= quiz.getMaxAttempts()) {
            throw new BusinessRuleException("Attempt limit " + quiz.getMaxAttempts() + " reached for quiz " + quizId);
        }

        List<Question> questions = questionRepository.findWithOptionsByQuizId(quizId);
        QuizResult result = quizGrader.grade(questions, request.answers(), quiz.getPassingScore());
        QuizSubmission submission = new QuizSubmission(quiz, student, attempts + 1, clock.instant());
        submission.applyResult(result);
        quizSubmissionRepository.saveAndFlush(submission);
        eventPublisher.publishEvent(new QuizCompletedEvent(student.getId(), quiz.getTitle(), result.percentage(), result.passed()));
        log.info("Student id={} took quiz id={} attempt={} result={}%", student.getId(), quizId, attempts + 1, result.percentage());
        return QuizSubmissionResponse.from(submission);
    }

    public List<QuizSubmissionResponse> listByQuiz(Long quizId) {
        quizService.find(quizId);
        return quizSubmissionRepository.findByQuizIdOrderByTakenAtAsc(quizId).stream().map(QuizSubmissionResponse::from).toList();
    }

    public List<QuizSubmissionResponse> listByStudent(Long studentId) {
        userLookup.getStudent(studentId);
        return quizSubmissionRepository.findByStudentIdOrderByTakenAtDesc(studentId).stream()
                .map(QuizSubmissionResponse::from)
                .toList();
    }

    public List<QuizSubmissionResponse> listByCourse(Long courseId) {
        courseService.find(courseId);
        return quizSubmissionRepository.findByCourseId(courseId).stream().map(QuizSubmissionResponse::from).toList();
    }
}
