package io.github.nicoloylife.projektboard.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.nicoloylife.projektboard.domain.Project;
import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Task;
import io.github.nicoloylife.projektboard.domain.TaskStatus;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.security.SecurityConfig;
import io.github.nicoloylife.projektboard.service.ConflictException;
import io.github.nicoloylife.projektboard.service.TaskService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
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

@WebMvcTest({TaskController.class, AuthController.class})
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private CurrentUserService currentUserService;

    private User anna;
    private Task task;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant("LoyLife Coding GmbH");
        User leitung = new User("leitung", "hash", "Petra Lang", Role.PROJECT_MANAGER, tenant);
        anna = new User("anna", "hash", "Anna Berger", Role.EMPLOYEE, tenant);
        ReflectionTestUtils.setField(anna, "id", 3L);
        Project project = new Project("Kundenportal Relaunch", null, leitung, tenant);
        task = new Task("Neue Aufgabe", null, project, anna, LocalDate.of(2026, 10, 1));
        ReflectionTestUtils.setField(task, "id", 500L);
        given(currentUserService.require()).willReturn(anna);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createReturnsCreatedTask() throws Exception {
        given(taskService.create(eq(anna), eq(100L), eq("Neue Aufgabe"), isNull(), eq(3L), eq(LocalDate.of(2026, 10, 1))))
                .willReturn(task);

        mockMvc.perform(withCsrf(post("/api/projects/100/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neue Aufgabe\",\"assigneeId\":3,\"dueDate\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/500"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignee.displayName").value("Anna Berger"))
                .andExpect(jsonPath("$.dueDate").value("2026-10-01"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void blankTitleIsRejected() throws Exception {
        mockMvc.perform(withCsrf(post("/api/projects/100/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void statusChangeReturnsTask() throws Exception {
        task.setStatus(TaskStatus.DONE);
        given(taskService.changeStatus(anna, 500L, TaskStatus.DONE)).willReturn(task);

        mockMvc.perform(withCsrf(patch("/api/tasks/500/status"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void archivedProjectYieldsConflict() throws Exception {
        given(taskService.create(any(), any(), any(), any(), any(), any()))
                .willThrow(new ConflictException("Das Projekt ist archiviert"));

        mockMvc.perform(withCsrf(post("/api/projects/300/tasks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aufgabe\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("conflict"));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }
}
