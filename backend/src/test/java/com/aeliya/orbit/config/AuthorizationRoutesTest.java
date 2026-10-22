package com.aeliya.orbit.config;

import com.aeliya.orbit.health.HealthController;
import com.aeliya.orbit.modules.login.controller.LoginController;
import com.aeliya.orbit.modules.login.service.LoginService;
import com.aeliya.orbit.modules.permission.controller.PermissionController;
import com.aeliya.orbit.modules.permission.service.PermissionService;
import com.aeliya.orbit.modules.user.controller.CurrentUserController;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import com.aeliya.orbit.security.SessionAccountFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestContextManager;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {HealthController.class, LoginController.class, PermissionController.class, CurrentUserController.class})
@Import({SecurityConfig.class, SessionAccountFilter.class})
class AuthorizationRoutesTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomUserDetailsService userDetails;
    @MockitoBean PermissionService permissions;
    @MockitoBean UserRepository users;
    @MockitoBean LoginService login;

    @BeforeEach void setup() {
        when(userDetails.loadUserByUsername(anyString())).thenAnswer(invocation ->
            SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test void anonymousCannotReadProtectedRoutes() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/permissions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/birthdays")).andExpect(status().isUnauthorized());
    }
    @Test void loginRequiresCsrfButCsrfBootstrapIsPublic() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"x\",\"password\":\"y\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"x\",\"password\":\"y\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotReadOrChangeGrants() throws Exception {
        mvc.perform(get("/api/admin/permissions/users/1")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/permissions/roles/ROLE_ADMIN").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"permissions\":[]}"))
            .andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "user", authorities = "EMPLOYEE_VIEW")
    void permissionAloneCannotManageGrants() throws Exception {
        mvc.perform(get("/api/admin/permissions")).andExpect(status().isForbidden());
        mvc.perform(get("/api/unknown")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "user")
    void employeeManagementRequiresExplicitGrants() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees/count")).andExpect(status().isForbidden());
        mvc.perform(post("/api/employees").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/employees/1/deactivate").with(csrf()))
            .andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "root", roles = "SUPER_ADMIN")
    void superAdminCanReachGrantEndpoint() throws Exception {
        mvc.perform(get("/api/admin/permissions")).andExpect(status().isOk());
    }

    public static void main(String[] args) throws Exception {
        TestContextManager context = new TestContextManager(AuthorizationRoutesTest.class);
        context.beforeTestClass();
        AuthorizationRoutesTest test = new AuthorizationRoutesTest();
        context.prepareTestInstance(test);
        for (String name : new String[] {"anonymousCannotReadProtectedRoutes", "loginRequiresCsrfButCsrfBootstrapIsPublic",
                "adminCannotReadOrChangeGrants", "permissionAloneCannotManageGrants",
                "employeeManagementRequiresExplicitGrants", "superAdminCanReachGrantEndpoint"}) {
            var method = AuthorizationRoutesTest.class.getDeclaredMethod(name);
            context.beforeTestMethod(test, method);
            try { test.setup(); method.invoke(test); }
            catch (java.lang.reflect.InvocationTargetException e) { throw new AssertionError(name, e.getCause()); }
            finally { context.afterTestMethod(test, method, null); }
        }
        context.afterTestClass();
    }
}
