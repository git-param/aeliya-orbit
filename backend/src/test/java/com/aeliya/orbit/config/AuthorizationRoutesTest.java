package com.aeliya.orbit.config;

import com.aeliya.orbit.health.HealthController;
import com.aeliya.orbit.modules.login.controller.LoginController;
import com.aeliya.orbit.modules.login.service.LoginService;
import com.aeliya.orbit.modules.login.dto.LoginResponse;
import com.aeliya.orbit.modules.permission.controller.PermissionController;
import com.aeliya.orbit.modules.permission.service.PermissionService;
import com.aeliya.orbit.modules.user.controller.CurrentUserController;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import com.aeliya.orbit.security.JwtAuthenticationFilter;
import com.aeliya.orbit.security.JwtUtil;
import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestContextManager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = {HealthController.class, LoginController.class, PermissionController.class, CurrentUserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthorizationRoutesTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean CustomUserDetailsService userDetails;
    @MockitoBean PermissionService permissions;
    @MockitoBean UserRepository users;
    @MockitoBean LoginService login;

    @Test void anonymousCannotReadProtectedRoutes() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/permissions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isUnauthorized());
    }

    @Test void onlyPostLoginIsPublic() throws Exception {
        when(login.authenticate(any())).thenReturn(new LoginResponse("test-token", "ROLE_USER"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"x\",\"password\":\"y\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotReadOrChangeGrants() throws Exception {
        mvc.perform(get("/api/admin/permissions/users/1")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/permissions/roles/ROLE_ADMIN")
            .contentType(MediaType.APPLICATION_JSON).content("{\"permissions\":[]}"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(username = "user", roles = "USER", authorities = "EMPLOYEE_VIEW")
    void permissionAloneCannotManageGrants() throws Exception {
        mvc.perform(get("/api/admin/permissions")).andExpect(status().isForbidden());
        mvc.perform(get("/api/unknown")).andExpect(status().isForbidden());
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
        for (String name : new String[] {"anonymousCannotReadProtectedRoutes", "onlyPostLoginIsPublic",
                "adminCannotReadOrChangeGrants", "permissionAloneCannotManageGrants",
                "superAdminCanReachGrantEndpoint"}) {
            var method = AuthorizationRoutesTest.class.getDeclaredMethod(name);
            context.beforeTestMethod(test, method);
            try {
                method.invoke(test);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new AssertionError(name, e.getCause());
            } finally {
                context.afterTestMethod(test, method, null);
            }
        }
        context.afterTestClass();
    }
}
