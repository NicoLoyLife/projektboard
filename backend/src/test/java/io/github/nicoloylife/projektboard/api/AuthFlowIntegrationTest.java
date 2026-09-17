package io.github.nicoloylife.projektboard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Prüft den Anmeldeablauf über den gesamten Stack mit den Startdaten. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"anna\",\"password\":\"demo1234\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        Cookie csrf = csrfCookie(null);
        mockMvc.perform(withCsrf(post("/api/auth/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"anna\",\"password\":\"falsch\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void loginWithoutUsernameIsValidationError() throws Exception {
        Cookie csrf = csrfCookie(null);
        mockMvc.perform(withCsrf(post("/api/auth/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"demo1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("username"));
    }

    @Test
    void meWithoutSessionIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void loginCreatesSessionAndMeReturnsUser() throws Exception {
        MockHttpSession session = login("anna", "demo1234");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("anna"))
                .andExpect(jsonPath("$.displayName").value("Anna Berger"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void logoutEndsSession() throws Exception {
        MockHttpSession session = login("leitung", "demo1234");
        Cookie csrf = csrfCookie(session);

        mockMvc.perform(withCsrf(post("/api/auth/logout"), csrf).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        Cookie csrf = csrfCookie(null);
        MvcResult result = mockMvc.perform(withCsrf(post("/api/auth/login"), csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private Cookie csrfCookie(MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/auth/csrf");
        if (session != null) {
            request = request.session(session);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isNoContent()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        return cookie;
    }

    private static MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, Cookie csrf) {
        return request.cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
    }
}
