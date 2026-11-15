package com.ormcourse.platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.security.JwtProperties;
import com.ormcourse.platform.security.SecurityConfig;
import com.ormcourse.platform.support.ApiTest;
import com.ormcourse.platform.support.TestFixtures;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class SecurityApiTest extends ApiTest {

    private static final String INVALID_TOKEN = "Access token is invalid or expired";

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void publicCatalogIsAvailableWithoutToken() throws Exception {
        long courseId = fixtures.publishedCourse().course().id();

        anonymous.perform(get("/api/courses")).andExpect(status().isOk());
        anonymous.perform(get("/api/courses/{id}", courseId)).andExpect(status().isOk());
        anonymous.perform(get("/api/courses/{id}/reviews", courseId)).andExpect(status().isOk());
        anonymous.perform(get("/api/categories")).andExpect(status().isOk());
        anonymous.perform(get("/api/tags")).andExpect(status().isOk());
        anonymous.perform(get("/v3/api-docs")).andExpect(status().isOk());
        anonymous.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void draftCourseIsVisibleOnlyToItsTeacherAndAdmin() throws Exception {
        Long teacher = fixtures.teacher();
        long draft = fixtures.course(teacher, CourseStatus.DRAFT, 1, 1).course().id();
        long archived = fixtures.course(teacher, CourseStatus.ARCHIVED, 1, 1).course().id();
        String student = fixtures.bearer(fixtures.student());
        String colleague = fixtures.bearer(fixtures.teacher());

        anonymous.perform(get("/api/courses/{id}", draft)).andExpect(status().isNotFound());
        anonymous.perform(get("/api/courses/{id}/reviews", draft)).andExpect(status().isNotFound());
        anonymous.perform(as(student, get("/api/courses/{id}", draft))).andExpect(status().isNotFound());
        anonymous.perform(as(colleague, get("/api/courses/{id}", draft))).andExpect(status().isNotFound());
        anonymous.perform(get("/api/courses/{id}", archived)).andExpect(status().isOk());
        anonymous.perform(as(fixtures.bearer(teacher), get("/api/courses/{id}", draft))).andExpect(status().isOk());
        mockMvc.perform(get("/api/courses/{id}/reviews", draft)).andExpect(status().isOk());

        expectCatalogSize(anonymous.perform(get("/api/courses").param("teacherId", teacher.toString())), 1);
        expectCatalogSize(anonymous.perform(as(student, get("/api/courses").param("status", "DRAFT"))), 0);
        expectCatalogSize(anonymous.perform(as(colleague, get("/api/courses").param("teacherId", teacher.toString()))), 1);
        expectCatalogSize(anonymous.perform(as(fixtures.bearer(teacher), get("/api/courses"))), 2);
        expectCatalogSize(mockMvc.perform(get("/api/courses")), 2);
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorizedProblem() throws Exception {
        anonymous.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.instance").value("/api/users"));
        anonymous.perform(postJson("/api/categories", "{\"name\": \"Hack\"}")).andExpect(status().isUnauthorized());
        anonymous.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedForgedAndExpiredTokensAreRejected() throws Exception {
        Long student = fixtures.student();
        Instant past = Instant.now().minus(Duration.ofHours(3));
        String expired = token(student, jwtProperties.issuer(), past, past.plus(Duration.ofHours(1)));
        String foreignIssuer = token(student, "someone-else", Instant.now(), Instant.now().plus(Duration.ofHours(1)));

        for (String bearer : new String[] {"Bearer not-a-jwt", expired, foreignIssuer}) {
            anonymous.perform(as(bearer, get("/api/auth/me")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value(INVALID_TOKEN));
        }

        anonymous.perform(as(fixtures.bearer(student), get("/api/auth/me"))).andExpect(status().isOk());
    }

    @Test
    void registrationCreatesStudentAndLoginIssuesToken() throws Exception {
        String body = """
                {"name": "Newbie", "email": "New@Mail.io", "password": "%s", "role": "ADMIN"}
                """.formatted(TestFixtures.PASSWORD);

        anonymous.perform(postJson("/api/auth/register", body))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        anonymous.perform(postJson("/api/auth/register", body)).andExpect(status().isConflict());
        anonymous.perform(postJson("/api/auth/register", body.replace(TestFixtures.PASSWORD, "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        anonymous.perform(postJson("/api/auth/login", "{\"email\": \"new@mail.io\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        anonymous.perform(postJson("/api/auth/login", "{\"email\": \"ghost@mail.io\", \"password\": \"whatever1\"}"))
                .andExpect(status().isUnauthorized());
        anonymous.perform(postJson("/api/auth/login", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(jwtProperties.ttl().toSeconds()))
                .andExpect(jsonPath("$.user.email").value("new@mail.io"));
        anonymous.perform(as(login("new@mail.io"), get("/api/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.io"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void studentCannotUseTeacherOrAdminFunctions() throws Exception {
        Long student = fixtures.student();
        String bearer = fixtures.bearer(student);
        long courseId = fixtures.publishedCourse().course().id();
        String course = courseBody(fixtures.category(), student);

        expectForbidden(as(bearer, postJson("/api/courses", course)));
        expectForbidden(as(bearer, postJson("/api/categories", "{\"name\": \"Mine\"}")));
        expectForbidden(as(bearer, postJson("/api/tags", "{\"name\": \"mine\"}")));
        expectForbidden(as(bearer, get("/api/users")));
        expectForbidden(as(bearer, delete("/api/courses/{id}", courseId)));
        expectForbidden(as(bearer, get("/api/courses/{id}/students", courseId)));
        expectForbidden(as(bearer, get("/api/analytics/courses/{id}", courseId)));
    }

    @Test
    void studentCannotSeeAnswersOrGradeWork() throws Exception {
        var course = fixtures.publishedCourse();
        Long student = fixtures.student();
        String bearer = fixtures.bearer(student);
        long quizId = fixtures.quiz(fixtures.firstModuleId(course), 3).id();
        fixtures.enroll(course.course().id(), student);
        long submissionId = fixtures.submit(fixtures.assignment(course, null).id(), student).id();

        mockMvc.perform(as(bearer, get("/api/quizzes/{id}", quizId))).andExpect(status().isOk());
        expectForbidden(as(bearer, get("/api/quizzes/{id}", quizId).param("includeAnswers", "true")));
        expectForbidden(as(bearer, putJson("/api/submissions/{id}/grade", "{\"score\": 100}", submissionId)));
        mockMvc.perform(as(bearer, get("/api/submissions/{id}", submissionId))).andExpect(status().isOk());
    }

    @Test
    void teacherManagesOnlyOwnCourses() throws Exception {
        Long teacher = fixtures.teacher();
        Long colleague = fixtures.teacher();
        String bearer = fixtures.bearer(teacher);
        long foreignCourse = fixtures.course(colleague, CourseStatus.PUBLISHED, 1, 1).course().id();
        long category = fixtures.category();

        mockMvc.perform(as(bearer, postJson("/api/courses", courseBody(category, teacher)))).andExpect(status().isCreated());
        expectForbidden(as(bearer, postJson("/api/courses", courseBody(category, colleague))));
        expectForbidden(as(bearer, putJson("/api/courses/{id}", courseBody(category, teacher), foreignCourse)));
        expectForbidden(as(bearer, postJson("/api/courses/{id}/modules", "{\"title\": \"Extra\"}", foreignCourse)));
        expectForbidden(as(bearer, delete("/api/courses/{id}", foreignCourse)));
        expectForbidden(as(bearer, get("/api/analytics/courses/{id}", foreignCourse)));
        mockMvc.perform(as(bearer, putJson("/api/courses/{id}", courseBody(category, teacher), Long.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentActsOnlyOnOwnBehalf() throws Exception {
        long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();
        Long other = fixtures.student();
        String bearer = fixtures.bearer(student);

        mockMvc.perform(as(bearer, post("/api/courses/{id}/enroll", courseId).param("user", student.toString())))
                .andExpect(status().isCreated());
        expectForbidden(as(bearer, post("/api/courses/{id}/enroll", courseId).param("user", other.toString())));
        expectForbidden(as(bearer, get("/api/analytics/students/{id}/progress", other)));
        expectForbidden(as(bearer, get("/api/users/{id}/notifications", other)));
        expectForbidden(as(bearer, get("/api/users/{id}/enrollments", other)));
        expectForbidden(as(bearer, putJson("/api/users/{id}", "{\"name\": \"X\", \"email\": \"x@x.io\"}", other)));
        mockMvc.perform(as(bearer, get("/api/analytics/students/{id}/progress", student))).andExpect(status().isOk());
        mockMvc.perform(as(fixtures.bearer(fixtures.teacher()), get("/api/analytics/students/{id}/progress", other)))
                .andExpect(status().isOk());
    }

    private void expectForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    private static void expectCatalogSize(ResultActions result, int size) throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(size));
    }

    private static String courseBody(long categoryId, long teacherId) {
        return """
                {"title": "Course", "categoryId": %d, "teacherId": %d, "status": "DRAFT"}
                """.formatted(categoryId, teacherId);
    }

    private String token(Long userId, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(userId.toString())
                .claim(SecurityConfig.ROLE_CLAIM, "ADMIN")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
