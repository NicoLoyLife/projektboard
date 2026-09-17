package io.github.nicoloylife.projektboard.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.ProjectStatus;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.security.SecurityConfig;
import io.github.nicoloylife.projektboard.service.ForbiddenException;
import io.github.nicoloylife.projektboard.service.NotFoundException;
import io.github.nicoloylife.projektboard.service.ProjectService;
import io.github.nicoloylife.projektboard.service.TaskCounts;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Prüft Rollenregeln, Validierung und Fehlerabbildung der Projekt-API mit gemocktem Service. */
@WebMvcTest({ProjectController.class, AuthController.class})
@Import(SecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProjectService projectService;
    @MockitoBean
    private CurrentUserService currentUserService;

    private User leitung;
    private Project relaunch;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant("LoyLife Coding GmbH");
        ReflectionTestUtils.setField(tenant, "id", 1L);
        leitung = new User("leitung", "hash", "Petra Lang", Role.PROJECT_MANAGER, tenant);
        ReflectionTestUtils.setField(leitung, "id", 2L);
        relaunch = new Project("Kundenportal Relaunch", "Beschreibung", leitung, tenant);
        ReflectionTestUtils.setField(relaunch, "id", 100L);
        given(currentUserService.require()).willReturn(leitung);
        given(projectService.tasksOf(any())).willReturn(List.of());
    }

    @Test
    void listRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void listContainsProgress() throws Exception {
        given(projectService.listVisible(leitung)).willReturn(List.of(relaunch));
        given(projectService.countsFor(List.of(relaunch))).willReturn(Map.of(100L, new TaskCounts(5, 2, 1, 2)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kundenportal Relaunch"))
                .andExpect(jsonPath("$[0].manager.displayName").value("Petra Lang"))
                .andExpect(jsonPath("$[0].counts.done").value(2))
                .andExpect(jsonPath("$[0].progressPercent").value(40));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeesMayNotCreateProjects() throws Exception {
        mockMvc.perform(withCsrf(post("/api/projects"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Neu\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithMockUser(roles = "PROJECT_MANAGER")
    void projectManagerCreatesProject() throws Exception {
        given(projectService.create(eq(leitung), eq("Neu"), eq("Text"))).willReturn(relaunch);
        given(projectService.countsFor(any())).willReturn(Map.of());

        mockMvc.perform(withCsrf(post("/api/projects"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Neu\",\"description\":\"Text\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/100"))
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.progressPercent").value(0));
    }

    @Test
    @WithMockUser(roles = "PROJECT_MANAGER")
    void blankNameIsRejected() throws Exception {
        mockMvc.perform(withCsrf(post("/api/projects"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @WithMockUser(roles = "PROJECT_MANAGER")
    void archivingReturnsUpdatedDetail() throws Exception {
        relaunch.setStatus(ProjectStatus.ARCHIVED);
        given(projectService.changeStatus(leitung, 100L, ProjectStatus.ARCHIVED)).willReturn(relaunch);
        given(projectService.countsFor(any())).willReturn(Map.of());

        mockMvc.perform(withCsrf(patch("/api/projects/100/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void serviceExceptionsAreMappedToStatusCodes() throws Exception {
        given(projectService.getVisible(leitung, 404L)).willThrow(new NotFoundException("Projekt nicht gefunden"));
        given(projectService.changeStatus(leitung, 100L, ProjectStatus.ARCHIVED))
                .willThrow(new ForbiddenException("Nur die Projektleitung darf das Projekt ändern"));

        mockMvc.perform(get("/api/projects/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
        mockMvc.perform(withCsrf(patch("/api/projects/100/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Nur die Projektleitung darf das Projekt ändern"));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }
}
