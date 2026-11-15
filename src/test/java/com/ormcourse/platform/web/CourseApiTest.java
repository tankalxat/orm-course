package com.ormcourse.platform.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ormcourse.platform.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class CourseApiTest extends ApiTest {

    @Test
    void createReturnsCreatedWithLocationAndNestedStructure() throws Exception {
        String body = """
                {"title": "JPA", "categoryId": %d, "teacherId": %d, "status": "PUBLISHED",
                 "tagIds": [%d],
                 "modules": [{"title": "Mapping", "lessons": [{"title": "Entities"}, {"title": "Relations"}]}]}
                """.formatted(fixtures.category(), fixtures.teacher(), fixtures.tag("jpa"));

        MvcResult result = mockMvc.perform(postJson("/api/courses", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.course.title").value("JPA"))
                .andExpect(jsonPath("$.course.tags", contains("jpa")))
                .andExpect(jsonPath("$.modules[0].lessons", hasSize(2)))
                .andExpect(jsonPath("$.modules[0].lessons[1].orderIndex").value(2))
                .andReturn();

        long id = read(result, "$.course.id");
        assertThat(result.getResponse().getHeader("Location")).endsWith("/api/courses/" + id);
        mockMvc.perform(get("/api/courses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.id").value(id));
    }

    @Test
    void invalidBodyReturnsValidationProblem() throws Exception {
        mockMvc.perform(postJson("/api/courses", "{\"title\": \" \", \"durationHours\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value("Request body is invalid"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists())
                .andExpect(jsonPath("$.errors.teacherId").exists())
                .andExpect(jsonPath("$.errors.durationHours").exists());
    }

    @Test
    void unknownCourseReturnsNotFoundProblem() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.instance").value("/api/courses/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void updateAndDeleteCourse() throws Exception {
        long id = fixtures.publishedCourse().course().id();
        String body = """
                {"title": "Updated", "categoryId": %d, "teacherId": %d, "status": "ARCHIVED"}
                """.formatted(fixtures.category(), fixtures.teacher());

        mockMvc.perform(putJson("/api/courses/{id}", body, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.title").value("Updated"))
                .andExpect(jsonPath("$.course.status").value("ARCHIVED"));
        mockMvc.perform(delete("/api/courses/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/courses/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void courseWithStudentsCannotBeDeleted() throws Exception {
        long id = fixtures.publishedCourse().course().id();
        fixtures.enroll(id, fixtures.student());

        mockMvc.perform(delete("/api/courses/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void tagsAndModulesAreManagedThroughSubresources() throws Exception {
        long id = fixtures.publishedCourse().course().id();
        long tag = fixtures.tag("orm");

        mockMvc.perform(put("/api/courses/{id}/tags/{tagId}", id, tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", contains("orm")));
        mockMvc.perform(delete("/api/courses/{id}/tags/{tagId}", id, tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(0)));
        MvcResult module = mockMvc.perform(postJson("/api/courses/{id}/modules", "{\"title\": \"Extra\"}", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderIndex").value(2))
                .andReturn();

        assertThat(module.getResponse().getHeader("Location")).endsWith("/api/modules/" + read(module, "$.id"));
    }

    @Test
    void searchSupportsFiltersAndPaging() throws Exception {
        fixtures.publishedCourse();
        fixtures.publishedCourse();

        mockMvc.perform(get("/api/courses").param("status", "PUBLISHED").param("size", "1").param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/courses").param("status", "DRAFT"))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
