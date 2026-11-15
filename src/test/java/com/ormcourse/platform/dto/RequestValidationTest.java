package com.ormcourse.platform.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ormcourse.platform.dto.quiz.AnswerOptionRequest;
import com.ormcourse.platform.dto.quiz.QuestionRequest;
import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.entity.QuestionType;
import com.ormcourse.platform.entity.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @CsvSource({
            "SINGLE_CHOICE, true, false, true",
            "SINGLE_CHOICE, true, true, false",
            "SINGLE_CHOICE, false, false, false",
            "MULTIPLE_CHOICE, true, true, true",
            "MULTIPLE_CHOICE, false, false, false"
    })
    void questionMustHaveConsistentCorrectOptions(QuestionType type, boolean first, boolean second, boolean valid) {
        QuestionRequest request = new QuestionRequest("Q", type, List.of(
                new AnswerOptionRequest("a", first),
                new AnswerOptionRequest("b", second)
        ));

        assertThat(validator.validate(request).isEmpty()).isEqualTo(valid);
    }

    @Test
    void questionNeedsAtLeastTwoOptions() {
        QuestionRequest request = new QuestionRequest("Q", QuestionType.SINGLE_CHOICE, List.of(new AnswerOptionRequest("a", true)));

        assertThat(fields(validator.validate(request))).containsExactly("options");
    }

    @Test
    void ratingMustBeBetweenOneAndFive() {
        assertThat(fields(validator.validate(new ReviewRequest(1L, 6, null)))).containsExactly("rating");
        assertThat(fields(validator.validate(new ReviewRequest(1L, 0, null)))).containsExactly("rating");
        assertThat(validator.validate(new ReviewRequest(1L, 5, null))).isEmpty();
    }

    @Test
    void userRequestValidatesEmailAndPhone() {
        UserCreateRequest request = new UserCreateRequest("Ann", "not-email", "short", Role.STUDENT, "call me");

        assertThat(fields(validator.validate(request))).containsExactlyInAnyOrder("email", "password", "phone");
    }

    private static <T> List<String> fields(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString()).toList();
    }
}
