package io.github.nicoloylife.projektboard.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.nicoloylife.projektboard.domain.Role;
import io.github.nicoloylife.projektboard.domain.Tenant;
import io.github.nicoloylife.projektboard.domain.User;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.security.SecurityConfig;
import io.github.nicoloylife.projektboard.service.UserService;
import jakarta.servlet.http.Cookie;
import java.util.List;
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

/**
 * Prüft Rollenregeln und Validierung der Benutzer-API mit gemocktem Service. Der AuthController ist
 * mit geladen, damit das CSRF-Token wie im Browser über /api/auth/csrf bezogen werden kann.
 */
@WebMvcTest({UserController.class, AuthController.class})
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CurrentUserService currentUserService;

    private User admin;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant("LoyLife Coding GmbH");
        admin = new User("admin", "hash", "Administration", Role.ADMIN, tenant);
        ReflectionTestUtils.setField(admin, "id", 1L);
        given(currentUserService.require()).willReturn(admin);
    }

    @Test
    void listRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    @WithMockUser(roles = "PROJECT_MANAGER")
    void listIsForbiddenForProjectManagers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminReceivesUserList() throws Exception {
        User anna = new User("anna", "hash", "Anna Berger", Role.EMPLOYEE, admin.getTenant());
        ReflectionTestUtils.setField(anna, "id", 2L);
        given(userService.listUsers(admin)).willReturn(List.of(admin, anna));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].username").value("anna"))
                .andExpect(jsonPath("$[1].role").value("EMPLOYEE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesUser() throws Exception {
        User created = new User("neu", "hash", "Neue Person", Role.EMPLOYEE, admin.getTenant());
        ReflectionTestUtils.setField(created, "id", 7L);
        given(userService.createUser(eq(admin), eq("neu"), eq("geheim123"), eq("Neue Person"), eq(Role.EMPLOYEE)))
                .willReturn(created);

        mockMvc.perform(withCsrf(post("/api/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"neu\",\"password\":\"geheim123\",\"displayName\":\"Neue Person\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/7"))
                .andExpect(jsonPath("$.username").value("neu"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidUsernameIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(withCsrf(post("/api/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"A\",\"password\":\"kurz\",\"displayName\":\"\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'username')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'displayName')]").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"neu\",\"password\":\"geheim123\",\"displayName\":\"Neue Person\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        return request.cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
    }
}
