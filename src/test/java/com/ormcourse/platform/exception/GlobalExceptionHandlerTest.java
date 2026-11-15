package com.ormcourse.platform.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorDoesNotLeakDetails() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("password=secret"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Unexpected server error");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    void dataIntegrityViolationIsConflictWithoutSqlDetails() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("x", new SQLException("duplicate key users_email"));

        ProblemDetail problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).doesNotContain("users_email");
    }

    @Test
    void notFoundMessageNamesEntityAndId() {
        ProblemDetail problem = handler.handleNotFound(NotFoundException.of("Course", 7L));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Course with id 7 not found");
    }
}
