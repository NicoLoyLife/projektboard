package io.github.nicoloylife.projektboard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Prüft die Benutzerverwaltung über die API mit den Startdaten und echter Anmeldung, vor allem dass
 * Deaktivierung und Rollenwechsel sofort wirken, auch auf eine bestehende Sitzung.
 * Die Testtransaktion rollt Änderungen am Ende zurück, damit die Startdaten für andere Tests erhalten bleiben.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deactivationEndsExistingSession() throws Exception {
        MockHttpSession admin = login("admin");
        MockHttpSession anna = login("anna");
        long relaunchId = projectId(anna, "Kundenportal Relaunch");

        updateUser(admin, "anna", "EMPLOYEE", false);

        mockMvc.perform(get("/api/projects").session(anna))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Anmeldung erforderlich"));
        mockMvc.perform(withCsrf(post("/api/projects/" + relaunchId + "/tasks"), anna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aufgabe nach Deaktivierung\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeEndsExistingSession() throws Exception {
        MockHttpSession admin = login("admin");
        createUser(admin, "zweitadmin", "demo1234", "ADMIN");
        MockHttpSession zweitadmin = login("zweitadmin");

        updateUser(admin, "zweitadmin", "EMPLOYEE", true);

        mockMvc.perform(withCsrf(post("/api/users"), zweitadmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson("neuadmin", "demo1234", "ADMIN")))
                .andExpect(status().isUnauthorized());
        assertThat(usernames(admin)).doesNotContain("neuadmin");
    }

    @Test
    void passwordOver72BytesIsRejectedAsFieldError() throws Exception {
        MockHttpSession admin = login("admin");

        mockMvc.perform(withCsrf(post("/api/users"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson("langespasswort", "a".repeat(80), "EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    private void createUser(MockHttpSession admin, String username, String password, String role) throws Exception {
        mockMvc.perform(withCsrf(post("/api/users"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUserJson(username, password, role)))
                .andExpect(status().isCreated());
    }

    private void updateUser(MockHttpSession admin, String username, String role, boolean active) throws Exception {
        mockMvc.perform(withCsrf(put("/api/users/" + userId(admin, username)), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Testkonto\",\"role\":\"" + role + "\",\"active\":" + active + "}"))
                .andExpect(status().isOk());
    }

    private static String newUserJson(String username, String password, String role) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password
                + "\",\"displayName\":\"Testkonto\",\"role\":\"" + role + "\"}";
    }

    private long userId(MockHttpSession admin, String username) throws Exception {
        List<Integer> ids = JsonPath.read(usersJson(admin), "$[?(@.username == '" + username + "')].id");
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }

    private List<String> usernames(MockHttpSession admin) throws Exception {
        return JsonPath.read(usersJson(admin), "$[*].username");
    }

    private String usersJson(MockHttpSession admin) throws Exception {
        return mockMvc.perform(get("/api/users").session(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long projectId(MockHttpSession session, String name) throws Exception {
        String body = mockMvc.perform(get("/api/projects").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[?(@.name == '" + name + "')].id");
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }

    private MockHttpSession login(String username) throws Exception {
        Cookie csrf = csrfCookie(null);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, MockHttpSession session)
            throws Exception {
        Cookie csrf = csrfCookie(session);
        return request.session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
    }

    private Cookie csrfCookie(MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/auth/csrf");
        if (session != null) {
            request = request.session(session);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isNoContent()).andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN");
    }
}
