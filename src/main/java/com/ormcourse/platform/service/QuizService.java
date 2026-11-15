package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.quiz.QuestionRequest;
import com.ormcourse.platform.dto.quiz.QuestionResponse;
import com.ormcourse.platform.dto.quiz.QuizRequest;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.entity.CourseModule;
import com.ormcourse.platform.entity.Question;
import com.ormcourse.platform.entity.Quiz;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.QuestionRepository;
import com.ormcourse.platform.repository.QuizRepository;
import com.ormcourse.platform.repository.QuizSubmissionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final ModuleService moduleService;

    @Transactional
    public QuizResponse create(Long moduleId, QuizRequest request) {
        CourseModule module = moduleService.find(moduleId);

        if (quizRepository.existsByModuleId(moduleId)) {
            throw new ConflictException("Module " + moduleId + " already has a quiz");
        }

        Quiz quiz = new Quiz(
                request.title(),
                request.timeLimitMinutes(),
                request.passingScoreOrDefault(),
                request.maxAttemptsOrDefault()
        );
        List<QuestionRequest> questions = request.questionsOrEmpty();

        for (int i = 0; i < questions.size(); i++) {
            quiz.addQuestion(CourseStructureFactory.buildQuestion(questions.get(i), i + 1));
        }

        module.attachQuiz(quiz);
        quizRepository.save(quiz);
        log.info("Created quiz id={} for module id={} with {} questions", quiz.getId(), moduleId, questions.size());
        return QuizResponse.from(quiz, quiz.getQuestions(), true);
    }

    public QuizResponse get(Long quizId, boolean includeAnswers) {
        Quiz quiz = find(quizId);
        return QuizResponse.from(quiz, questionRepository.findWithOptionsByQuizId(quizId), includeAnswers);
    }

    public QuizResponse getByModule(Long moduleId, boolean includeAnswers) {
        Quiz quiz = quizRepository.findByModuleId(moduleId)
                .orElseThrow(() -> new NotFoundException("Quiz for module " + moduleId + " not found"));
        return get(quiz.getId(), includeAnswers);
    }

    @Transactional
    public QuestionResponse addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = find(quizId);
        requireNoAttempts(quizId);
        Question question = CourseStructureFactory.buildQuestion(request, questionRepository.findMaxOrderIndex(quizId) + 1);
        quiz.addQuestion(question);
        questionRepository.save(question);
        return QuestionResponse.from(question, true);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findWithQuizById(questionId)
                .orElseThrow(() -> NotFoundException.of("Question", questionId));
        requireNoAttempts(question.getQuiz().getId());
        question.getQuiz().removeQuestion(question);
    }

    @Transactional
    public void delete(Long quizId) {
        Quiz quiz = find(quizId);
        quiz.getModule().detachQuiz();
        quizRepository.delete(quiz);
        log.info("Deleted quiz id={}", quizId);
    }

    Quiz find(Long quizId) {
        return quizRepository.findWithCourseById(quizId).orElseThrow(() -> NotFoundException.of("Quiz", quizId));
    }

    private void requireNoAttempts(Long quizId) {
        if (quizSubmissionRepository.existsByQuizId(quizId)) {
            throw new ConflictException("Quiz " + quizId + " already has attempts; questions cannot be changed");
        }
    }
}
