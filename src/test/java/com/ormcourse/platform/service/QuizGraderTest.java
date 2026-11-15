package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.entity.AnswerOption;
import com.ormcourse.platform.entity.Question;
import com.ormcourse.platform.entity.QuestionType;
import com.ormcourse.platform.entity.QuizResult;
import com.ormcourse.platform.exception.BusinessRuleException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizGraderTest {

    private static final int PASSING_SCORE = 60;

    private final QuizGrader grader = new QuizGrader();
    private final Question single = question(1L, QuestionType.SINGLE_CHOICE, option(11L, true), option(12L, false));
    private final Question multiple = question(2L, QuestionType.MULTIPLE_CHOICE, option(21L, true), option(22L, true), option(23L, false));
    private final List<Question> questions = List.of(single, multiple);

    @Test
    void allCorrectAnswersGiveFullScore() {
        QuizResult result = grader.grade(questions, Map.of(1L, Set.of(11L), 2L, Set.of(21L, 22L)), PASSING_SCORE);

        assertThat(result.correctAnswers()).isEqualTo(2);
        assertThat(result.percentage()).isEqualTo(100);
        assertThat(result.passed()).isTrue();
        assertThat(result.selectedOptions()).extracting(AnswerOption::getId).containsExactlyInAnyOrder(11L, 21L, 22L);
    }

    @Test
    void partiallyCorrectMultipleChoiceIsCountedAsWrong() {
        QuizResult result = grader.grade(questions, Map.of(1L, Set.of(11L), 2L, Set.of(21L)), PASSING_SCORE);

        assertThat(result.correctAnswers()).isEqualTo(1);
        assertThat(result.percentage()).isEqualTo(50);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void unansweredQuestionsAreWrong() {
        QuizResult result = grader.grade(questions, Map.of(), PASSING_SCORE);

        assertThat(result.correctAnswers()).isZero();
        assertThat(result.totalQuestions()).isEqualTo(2);
        assertThat(result.selectedOptions()).isEmpty();
    }

    @Test
    void passingScoreBoundaryIsInclusive() {
        assertThat(grader.grade(questions, Map.of(1L, Set.of(11L)), 50).passed()).isTrue();
    }

    @Test
    void singleChoiceQuestionRejectsSeveralAnswers() {
        assertThatThrownBy(() -> grader.grade(questions, Map.of(1L, Set.of(11L, 12L)), PASSING_SCORE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only one answer");
    }

    @Test
    void optionsFromAnotherQuestionAreRejected() {
        assertThatThrownBy(() -> grader.grade(questions, Map.of(1L, Set.of(21L)), PASSING_SCORE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Options [21] do not belong to question 1");
    }

    @Test
    void questionsFromAnotherQuizAreRejected() {
        assertThatThrownBy(() -> grader.grade(questions, Map.of(99L, Set.of(1L)), PASSING_SCORE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Questions [99] do not belong to this quiz");
    }

    @Test
    void emptyQuizCannotBeGraded() {
        assertThatThrownBy(() -> grader.grade(List.of(), Map.of(), PASSING_SCORE)).isInstanceOf(BusinessRuleException.class);
    }

    private static Question question(Long id, QuestionType type, AnswerOption... options) {
        Question question = new Question("Question " + id, type, id.intValue());
        ReflectionTestUtils.setField(question, "id", id);

        for (AnswerOption option : options) {
            question.addOption(option);
        }

        return question;
    }

    private static AnswerOption option(Long id, boolean correct) {
        AnswerOption option = new AnswerOption("Option " + id, correct);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }
}
