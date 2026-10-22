package com.aeliya.orbit.config;

import com.aeliya.orbit.health.HealthController;
import com.aeliya.orbit.modules.login.controller.LoginController;
import com.aeliya.orbit.modules.login.service.LoginService;
import com.aeliya.orbit.modules.employee.controller.OrganizationConfigController;
import com.aeliya.orbit.modules.employee.controller.SelfProfileController;
import com.aeliya.orbit.modules.employee.service.SelfProfileService;
import com.aeliya.orbit.modules.employee.service.OrganizationOptionService;
import com.aeliya.orbit.modules.permission.controller.PermissionController;
import com.aeliya.orbit.modules.permission.controller.DepartmentPermissionController;
import com.aeliya.orbit.modules.permission.service.DepartmentPermissionService;
import com.aeliya.orbit.modules.permission.service.PermissionService;
import com.aeliya.orbit.modules.user.controller.CurrentUserController;
import com.aeliya.orbit.modules.user.controller.PasswordManagementController;
import com.aeliya.orbit.modules.user.service.PasswordManagementService;
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

@WebMvcTest(controllers = {HealthController.class, LoginController.class, PermissionController.class, DepartmentPermissionController.class, CurrentUserController.class, PasswordManagementController.class, OrganizationConfigController.class, SelfProfileController.class})
@Import({SecurityConfig.class, SessionAccountFilter.class})
class AuthorizationRoutesTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomUserDetailsService userDetails;
    @MockitoBean PermissionService permissions;
    @MockitoBean UserRepository users;
    @MockitoBean LoginService login;
    @MockitoBean OrganizationOptionService organizationOptions;
    @MockitoBean DepartmentPermissionService departmentPermissions;
    @MockitoBean SelfProfileService selfProfiles;
    @MockitoBean PasswordManagementService passwordManagement;

    @BeforeEach void setup() {
        when(userDetails.loadUserByUsername(anyString())).thenAnswer(invocation ->
            SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test void anonymousCannotReadProtectedRoutes() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/permissions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/birthdays")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profile/aadhar")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/profile/picture").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/organization/options").param("kind", "DEPARTMENT")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/department-permissions")).andExpect(status().isUnauthorized());
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
        mvc.perform(get("/api/admin/department-permissions")).andExpect(status().isForbidden());
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
        mvc.perform(get("/api/employees/manager-options")).andExpect(status().isForbidden());
        mvc.perform(post("/api/employees/1/reactivate").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/1/password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\":\"new-password\"}")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "user")
    void authenticatedUserCanReachOwnProfileRoute() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isOk());
        mvc.perform(delete("/api/profile/picture").with(csrf())).andExpect(status().isNoContent());
    }
    @Test @WithMockUser(username = "user", authorities = {"ROLE_USER", "DEPARTMENT_MANAGE"})
    void ordinaryUserCannotManageOrganizationEvenWithGrant() throws Exception {
        mvc.perform(get("/api/admin/organization/options").param("kind", "DEPARTMENT"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/department-permissions")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/department-permissions/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"version\":0,\"moduleIds\":[\"102\"]}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/organization/options").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"LEVEL\",\"name\":\"L1\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/organization/options").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"kind\":\"LEVEL\",\"name\":\"L1\"}"))
            .andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void adminGrantRevocationRemovesConfigAccess() throws Exception {
        mvc.perform(get("/api/admin/organization/options").param("kind", "DEPARTMENT"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/department-permissions")).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "DEPARTMENT_MANAGE"})
    void adminWithGrantCanReadOrganizationSetup() throws Exception {
        mvc.perform(get("/api/admin/organization/options").param("kind", "DEPARTMENT"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/department-permissions")).andExpect(status().isOk());
    }
    @Test @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReachPasswordManagement() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isOk());
        mvc.perform(put("/api/admin/users/2/password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\":\"new-password\"}")).andExpect(status().isNoContent());
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
                "employeeManagementRequiresExplicitGrants", "authenticatedUserCanReachOwnProfileRoute", "ordinaryUserCannotManageOrganizationEvenWithGrant",
                "adminGrantRevocationRemovesConfigAccess", "adminWithGrantCanReadOrganizationSetup", "adminCanReachPasswordManagement",
                "superAdminCanReachGrantEndpoint"}) {
            var method = AuthorizationRoutesTest.class.getDeclaredMethod(name);
            context.beforeTestMethod(test, method);
            try { test.setup(); method.invoke(test); }
            catch (java.lang.reflect.InvocationTargetException e) { throw new AssertionError(name, e.getCause()); }
            finally { context.afterTestMethod(test, method, null); }
        }
        context.afterTestClass();
    }
}
