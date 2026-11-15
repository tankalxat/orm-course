package com.ormcourse.platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ormcourse.platform.support.ApiTest;
import org.junit.jupiter.api.Test;

class ErrorHandlingApiTest extends ApiTest {

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(postJson("/api/categories", "{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void pathVariableTypeMismatchReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"))
                .andExpect(jsonPath("$.detail").value("Parameter 'id' has invalid value 'abc'"));
    }

    @Test
    void invalidFilterParameterReturnsFieldError() throws Exception {
        mockMvc.perform(get("/api/courses").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request parameters are invalid"))
                .andExpect(jsonPath("$.errors.status").value("has invalid value 'UNKNOWN'"));
    }

    @Test
    void methodValidationOnQueryParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/analytics/courses/popular").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/categories"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String body = """
                {"name": "Ann", "email": "ann@test.io", "password": "password123", "role": "STUDENT"}
                """;
        mockMvc.perform(postJson("/api/users", body)).andExpect(status().isCreated());

        mockMvc.perform(postJson("/api/users", body.replace("ann@", "ANN@")))
                .andExpect(status().isConflict());
    }

    @Test
    void reviewWithoutEnrollmentViolatesBusinessRule() throws Exception {
        long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();
        String body = "{\"studentId\": %d, \"rating\": 4}".formatted(student);

        mockMvc.perform(as(fixtures.bearer(student), postJson("/api/courses/{id}/reviews", body, courseId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Business rule violated"));
    }

    @Test
    void profileIsValidatedAndUpserted() throws Exception {
        long user = fixtures.student();

        mockMvc.perform(get("/api/users/{id}/profile", user)).andExpect(status().isNotFound());
        mockMvc.perform(putJson("/api/users/{id}/profile", "{\"website\": \"not a url\"}", user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.website").exists());
        mockMvc.perform(putJson("/api/users/{id}/profile", "{\"city\": \"Kazan\", \"website\": \"https://ann.dev\"}", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user))
                .andExpect(jsonPath("$.city").value("Kazan"));
        mockMvc.perform(get("/api/users/{id}/profile", user)).andExpect(jsonPath("$.website").value("https://ann.dev"));
    }

    @Test
    void usersCatalogEndpointsWork() throws Exception {
        long teacher = fixtures.teacher();
        fixtures.student();

        mockMvc.perform(get("/api/users").param("role", "TEACHER"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(teacher));
        mockMvc.perform(putJson("/api/users/{id}", "{\"name\": \"Prof\", \"email\": \"prof@test.io\"}", teacher))
                .andExpect(jsonPath("$.name").value("Prof"));
        mockMvc.perform(get("/api/users").param("role", "GOD")).andExpect(status().isBadRequest());
        mockMvc.perform(postJson("/api/tags", "{\"name\": \"jpa\"}")).andExpect(status().isCreated());
        mockMvc.perform(get("/api/tags")).andExpect(jsonPath("$[0].name").value("jpa"));
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/users/{id}", teacher)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/{id}", teacher)).andExpect(status().isNotFound());
    }
}
