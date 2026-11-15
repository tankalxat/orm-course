package com.ormcourse.platform.web;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.support.ApiTest;
import com.ormcourse.platform.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class LearningFlowApiTest extends ApiTest {

    private long student;
    private String teacherToken;
    private String studentToken;
    private long courseId;
    private long moduleId;
    private long lessonId;
    private long enrollmentId;
    private long assignmentId;

    @Test
    void studentGoesThroughCourseFromEnrollmentToAnalytics() throws Exception {
        createCourse();
        enrollAndCompleteLesson();
        submitAndGradeAssignment();
        passQuizOnlyOnce();
        reviewAndCompleteCourse();
        verifyAnalytics();
        verifyStudentViews();
        verifyNotifications();
    }

    private void createCourse() throws Exception {
        long teacher = createUser("Teacher", "teacher@flow.io", "TEACHER");
        student = createUser("Student", "student@flow.io", "STUDENT");
        teacherToken = login("teacher@flow.io");
        studentToken = login("student@flow.io");
        long category = read(mockMvc.perform(postJson("/api/categories", "{\"name\": \"ORM\"}"))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
        MvcResult course = mockMvc.perform(as(teacherToken, postJson("/api/courses", """
                        {"title": "Hibernate", "categoryId": %d, "teacherId": %d, "status": "PUBLISHED",
                         "modules": [{"title": "Basics", "lessons": [{"title": "Session"}]}]}
                        """.formatted(category, teacher))))
                .andExpect(status().isCreated())
                .andReturn();
        courseId = read(course, "$.course.id");
        moduleId = read(course, "$.modules[0].id");
        lessonId = read(course, "$.modules[0].lessons[0].id");
    }

    private void enrollAndCompleteLesson() throws Exception {
        MockHttpServletRequestBuilder enroll = post("/api/courses/{id}/enroll", courseId).param("user", String.valueOf(student));
        enrollmentId = read(mockMvc.perform(as(studentToken, enroll))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn(), "$.id");
        mockMvc.perform(as(studentToken, post("/api/lessons/{id}/complete", lessonId).param("user", String.valueOf(student))))
                .andExpect(status().isNoContent());
    }

    private void submitAndGradeAssignment() throws Exception {
        assignmentId = read(mockMvc.perform(as(teacherToken, postJson("/api/lessons/{id}/assignments", """
                        {"title": "Map entities", "maxScore": 100}
                        """, lessonId)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
        long submissionId = read(mockMvc.perform(as(studentToken, postJson("/api/assignments/{id}/submit", """
                        {"studentId": %d, "content": "@Entity class Course {}"}
                        """.formatted(student), assignmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn(), "$.id");
        String grade = "{\"score\": 80, \"feedback\": \"Good\"}";
        mockMvc.perform(as(teacherToken, putJson("/api/submissions/{id}/grade", grade, submissionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRADED"))
                .andExpect(jsonPath("$.score").value(80));
    }

    private void passQuizOnlyOnce() throws Exception {
        MvcResult quiz = mockMvc.perform(as(teacherToken, postJson("/api/modules/{id}/quiz", """
                        {"title": "Check", "passingScore": 50, "maxAttempts": 1,
                         "questions": [{"text": "Default fetch of @OneToMany?", "type": "SINGLE_CHOICE",
                                        "options": [{"text": "LAZY", "correct": true}, {"text": "EAGER", "correct": false}]}]}
                        """, moduleId)))
                .andExpect(status().isCreated())
                .andReturn();
        long quizId = read(quiz, "$.id");
        mockMvc.perform(as(studentToken, get("/api/quizzes/{id}", quizId)))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist());
        String answers = """
                {"studentId": %d, "answers": {"%d": [%d]}}
                """.formatted(student, read(quiz, "$.questions[0].id"), read(quiz, "$.questions[0].options[0].id"));
        mockMvc.perform(as(studentToken, postJson("/api/quizzes/{id}/take", answers, quizId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.percentage").value(100));
        mockMvc.perform(as(studentToken, postJson("/api/quizzes/{id}/take", answers, quizId)))
                .andExpect(status().isUnprocessableEntity());
    }

    private void reviewAndCompleteCourse() throws Exception {
        String review = "{\"studentId\": %d, \"rating\": 5}".formatted(student);
        mockMvc.perform(as(studentToken, postJson("/api/courses/{id}/reviews", review, courseId)))
                .andExpect(status().isCreated());
        mockMvc.perform(as(teacherToken, patch("/api/enrollments/{id}", enrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETED\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").exists());
    }

    private void verifyAnalytics() throws Exception {
        mockMvc.perform(as(teacherToken, get("/api/analytics/courses/{id}", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedEnrollments").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.averageScorePercent").value(80.0))
                .andExpect(jsonPath("$.quizPassRatePercent").value(100.0));
        mockMvc.perform(as(teacherToken, get("/api/analytics/courses/{id}/leaderboard", courseId)))
                .andExpect(jsonPath("$[0].studentId").value(student));
        anonymous.perform(get("/api/analytics/courses/popular"))
                .andExpect(jsonPath("$[0].courseId").value(courseId));
        mockMvc.perform(as(studentToken, get("/api/analytics/students/{id}/progress", student)))
                .andExpect(jsonPath("$.courses[0].progressPercent").value(100))
                .andExpect(jsonPath("$.courses[0].quizzesPassed").value(1));
    }

    private void verifyStudentViews() throws Exception {
        mockMvc.perform(as(studentToken, get("/api/users/{id}/enrollments", student))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(as(studentToken, get("/api/users/{id}/assignments", student)))
                .andExpect(jsonPath("$[0].submission.score").value(80));
        mockMvc.perform(as(studentToken, get("/api/users/{id}/submissions", student))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(as(studentToken, get("/api/users/{id}/quiz-submissions", student))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(as(teacherToken, get("/api/courses/{id}/quiz-submissions", courseId))).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(as(teacherToken, get("/api/assignments/{id}/submissions", assignmentId))).andExpect(jsonPath("$", hasSize(1)));
        anonymous.perform(get("/api/courses/{id}/reviews", courseId)).andExpect(jsonPath("$[0].rating").value(5));
    }

    private void verifyNotifications() throws Exception {
        MockHttpServletRequestBuilder unread = get("/api/users/{id}/notifications", student).param("unreadOnly", "true");
        MvcResult notifications = mockMvc.perform(as(studentToken, unread))
                .andExpect(jsonPath("$", not(empty())))
                .andReturn();
        mockMvc.perform(as(studentToken, patch("/api/notifications/{id}/read", read(notifications, "$[0].id"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void contentCanBeEditedAndRemovedThroughApi() throws Exception {
        CourseDetailsResponse course = fixtures.publishedCourse();
        long moduleId = fixtures.firstModuleId(course);
        long lessonId = fixtures.firstLessonId(course);
        long assignmentId = fixtures.assignment(course, null).id();

        mockMvc.perform(putJson("/api/modules/{id}", "{\"title\": \"Renamed module\"}", moduleId))
                .andExpect(jsonPath("$.title").value("Renamed module"));
        mockMvc.perform(postJson("/api/modules/{id}/lessons", "{\"title\": \"Third\"}", moduleId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderIndex").value(3));
        mockMvc.perform(putJson("/api/lessons/{id}", "{\"title\": \"Renamed lesson\", \"videoUrl\": \"https://video.io/1\"}", lessonId))
                .andExpect(jsonPath("$.videoUrl").value("https://video.io/1"));
        mockMvc.perform(putJson("/api/assignments/{id}", "{\"title\": \"Renamed task\", \"maxScore\": 50}", assignmentId))
                .andExpect(jsonPath("$.maxScore").value(50));
        mockMvc.perform(get("/api/lessons/{id}/assignments", lessonId)).andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/assignments/{id}", assignmentId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/lessons/{id}", lessonId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/modules/{id}", moduleId)).andExpect(jsonPath("$.lessons", hasSize(2)));
        mockMvc.perform(delete("/api/modules/{id}", moduleId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/modules/{id}", moduleId)).andExpect(status().isNotFound());
    }

    private long createUser(String name, String email, String role) throws Exception {
        String body = """
                {"name": "%s", "email": "%s", "password": "%s", "role": "%s"}
                """.formatted(name, email, TestFixtures.PASSWORD, role);
        return read(mockMvc.perform(postJson("/api/users", body)).andExpect(status().isCreated()).andReturn(), "$.id");
    }
}
