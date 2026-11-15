package com.ormcourse.platform.support;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.jayway.jsonpath.JsonPath;
import com.ormcourse.platform.entity.Role;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

public abstract class ApiTest extends IntegrationTest {

    @Autowired
    protected MockMvc anonymous;

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mockMvc;
    protected Long adminId;

    @BeforeEach
    void authenticateAsAdmin() {
        adminId = fixtures.user(Role.ADMIN);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true, false))
                .apply(springSecurity())
                .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, fixtures.bearer(adminId)))
                .build();
    }

    protected static MockHttpServletRequestBuilder postJson(String url, String body, Object... vars) {
        return post(url, vars).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static MockHttpServletRequestBuilder putJson(String url, String body, Object... vars) {
        return put(url, vars).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static MockHttpServletRequestBuilder as(String bearer, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    protected String login(String email) throws Exception {
        String body = "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, TestFixtures.PASSWORD);
        MvcResult result = anonymous.perform(postJson("/api/auth/login", body)).andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    protected static long read(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value.longValue();
    }
}
