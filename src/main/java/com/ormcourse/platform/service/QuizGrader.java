package com.ormcourse.platform.service;

import com.ormcourse.platform.entity.AnswerOption;
import com.ormcourse.platform.entity.Question;
import com.ormcourse.platform.entity.QuestionType;
import com.ormcourse.platform.entity.QuizResult;
import com.ormcourse.platform.exception.BusinessRuleException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class QuizGrader {

    private static final int PERCENT = 100;

    public QuizResult grade(List<Question> questions, Map<Long, Set<Long>> answers, int passingScore) {
        if (questions.isEmpty()) {
            throw new BusinessRuleException("Quiz has no questions");
        }

        validateQuestionIds(questions, answers);
        Set<AnswerOption> selectedOptions = new HashSet<>();
        int correct = 0;

        for (Question question : questions) {
            Set<Long> selected = answers.getOrDefault(question.getId(), Set.of());
            selectedOptions.addAll(resolveOptions(question, selected));

            if (selected.equals(question.correctOptionIds())) {
                correct++;
            }
        }

        int percentage = Math.round(correct * (float) PERCENT / questions.size());
        return new QuizResult(correct, questions.size(), percentage, percentage >= passingScore, selectedOptions);
    }

    private static void validateQuestionIds(List<Question> questions, Map<Long, Set<Long>> answers) {
        Set<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        List<Long> unknown = answers.keySet().stream().filter(id -> !questionIds.contains(id)).sorted().toList();

        if (!unknown.isEmpty()) {
            throw new BusinessRuleException("Questions " + unknown + " do not belong to this quiz");
        }
    }

    private static List<AnswerOption> resolveOptions(Question question, Set<Long> selected) {
        if (question.getType() == QuestionType.SINGLE_CHOICE && selected.size() > 1) {
            throw new BusinessRuleException("Question " + question.getId() + " allows only one answer");
        }

        Map<Long, AnswerOption> options = question.getOptions().stream()
                .collect(Collectors.toMap(AnswerOption::getId, Function.identity()));
        List<Long> foreign = selected.stream().filter(id -> !options.containsKey(id)).sorted().toList();

        if (!foreign.isEmpty()) {
            throw new BusinessRuleException("Options " + foreign + " do not belong to question " + question.getId());
        }

        return selected.stream().map(options::get).toList();
    }
}
