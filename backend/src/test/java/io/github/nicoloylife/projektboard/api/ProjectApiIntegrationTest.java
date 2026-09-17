package io.github.nicoloylife.projektboard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Prüft Sichtbarkeit, Aufgaben und Fortschritt über die API mit den Startdaten und echter Anmeldung.
 * Die Testtransaktion rollt Änderungen am Ende zurück, damit die Startdaten für andere Tests erhalten bleiben.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void visibilityFollowsMembershipAndRole() throws Exception {
        assertThat(projectNames(login("anna"))).containsExactly("Interne Migration", "Kundenportal Relaunch");
        assertThat(projectNames(login("ben"))).containsExactly("Kundenportal Relaunch", "Website 2025");
        assertThat(projectNames(login("chris"))).isEmpty();
        assertThat(projectNames(login("leitung"))).hasSize(3);
        assertThat(projectNames(login("admin"))).hasSize(3);
    }

    @Test
    void memberCreatesTaskAndProgressFollowsStatus() throws Exception {
        MockHttpSession anna = login("anna");
        long relaunchId = projectId(anna, "Kundenportal Relaunch");

        mockMvc.perform(get("/api/projects/" + relaunchId).session(anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(40))
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.canEditTasks").value(true))
                .andExpect(jsonPath("$.tasks.length()").value(5));

        MvcResult created = mockMvc.perform(withCsrf(post("/api/projects/" + relaunchId + "/tasks"), anna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neue Aufgabe\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        int taskId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        MockHttpSession ben = login("ben");
        mockMvc.perform(withCsrf(patch("/api/tasks/" + taskId + "/status"), ben)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/projects/" + relaunchId).session(login("leitung")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.total").value(6))
                .andExpect(jsonPath("$.counts.done").value(3))
                .andExpect(jsonPath("$.progressPercent").value(50))
                .andExpect(jsonPath("$.canManage").value(true));
    }

    @Test
    void archivedProjectRejectsTasksAndOutsidersSeeNothing() throws Exception {
        MockHttpSession ben = login("ben");
        long websiteId = projectId(ben, "Website 2025");
        mockMvc.perform(withCsrf(post("/api/projects/" + websiteId + "/tasks"), ben)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nachtrag\"}"))
                .andExpect(status().isConflict());

        MockHttpSession chris = login("chris");
        mockMvc.perform(get("/api/projects/" + websiteId).session(chris))
                .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(post("/api/projects/" + websiteId + "/tasks"), chris)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fremd\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminReadsProjectsButCannotEditTasks() throws Exception {
        MockHttpSession admin = login("admin");
        long relaunchId = projectId(admin, "Kundenportal Relaunch");

        mockMvc.perform(get("/api/projects/" + relaunchId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canEditTasks").value(false));
        mockMvc.perform(withCsrf(post("/api/projects/" + relaunchId + "/tasks"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aufgabe\"}"))
                .andExpect(status().isForbidden());
    }

    private List<String> projectNames(MockHttpSession session) throws Exception {
        String body = mockMvc.perform(get("/api/projects").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].name");
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
