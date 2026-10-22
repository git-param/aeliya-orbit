package com.aeliya.orbit.modules.permission.service;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PermissionServiceTest {
    @Test void departmentModulesOverrideLegacyEmployeeGrants() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DepartmentModuleCache cache = mock(DepartmentModuleCache.class);
        PermissionService service = new PermissionService(jdbc, mock(UserRepository.class), cache);
        User user = new User("employee", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 5L);
        when(jdbc.queryForList(contains("role_permission_grants"), eq(String.class), eq("ROLE_USER")))
            .thenReturn(List.of("EMPLOYEE_VIEW"));
        when(jdbc.queryForList(contains("user_permission_grants"), eq(String.class), eq(5L)))
            .thenReturn(List.of("LEAVE_APPROVE"));
        when(cache.forAccount(5L)).thenReturn(Set.of("102"));
        when(cache.permissionCode("102")).thenReturn("EMPLOYEE_CREATE");
        assertEquals(Set.of("ROLE_USER", "MODULE_102", "EMPLOYEE_CREATE", "LEAVE_APPROVE"), service.authoritiesFor(user));
    }

    @Test void nonSuperAdminCannotChangeGrantsAndSuperAdminTargetIsHidden() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserRepository users = mock(UserRepository.class);
        PermissionService service = new PermissionService(jdbc, users, mock(DepartmentModuleCache.class));
        var admin = new UsernamePasswordAuthenticationToken("admin", null,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThrows(ResponseStatusException.class,
            () -> service.replaceRoleGrants("ROLE_ADMIN", Set.of("PAYROLL_VIEW"), admin));
        verifyNoInteractions(jdbc, users);

        User superAdmin = new User("root", "hash", "ROLE_SUPER_ADMIN");
        when(users.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(users.findById(2L)).thenReturn(Optional.empty());
        ResponseStatusException hidden = assertThrows(ResponseStatusException.class, () -> service.userGrants(1L));
        ResponseStatusException missing = assertThrows(ResponseStatusException.class, () -> service.userGrants(2L));
        assertEquals(missing.getStatusCode(), hidden.getStatusCode());
        verifyNoInteractions(jdbc);
    }

    @Test void departmentRevocationRemovesEmployeeAuthorityOnNextResolution() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DepartmentModuleCache cache = mock(DepartmentModuleCache.class);
        PermissionService service = new PermissionService(jdbc, mock(UserRepository.class), cache);
        User user = new User("member", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 9L);
        when(jdbc.queryForList(contains("role_permission_grants"), eq(String.class), eq("ROLE_USER")))
            .thenReturn(List.of("EMPLOYEE_CREATE"));
        when(jdbc.queryForList(contains("user_permission_grants"), eq(String.class), eq(9L)))
            .thenReturn(List.of("EMPLOYEE_CREATE"));
        when(cache.forAccount(9L)).thenReturn(Set.of("102"), Set.of());
        when(cache.permissionCode("102")).thenReturn("EMPLOYEE_CREATE");
        assertTrue(service.authoritiesFor(user).contains("EMPLOYEE_CREATE"));
        assertFalse(service.authoritiesFor(user).contains("EMPLOYEE_CREATE"));
    }

    @Test void adminCanCreateEmployeesByDefault() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PermissionService service = new PermissionService(jdbc, mock(UserRepository.class), mock(DepartmentModuleCache.class));
        User admin = new User("admin", "hash", "ROLE_ADMIN");
        ReflectionTestUtils.setField(admin, "id", 11L);
        assertTrue(service.authoritiesFor(admin).contains("EMPLOYEE_CREATE"));
        assertTrue(service.authoritiesFor(admin).contains("MODULE_102"));
    }

    public static void main(String[] args) {
        PermissionServiceTest test = new PermissionServiceTest();
        test.departmentModulesOverrideLegacyEmployeeGrants();
        test.nonSuperAdminCannotChangeGrantsAndSuperAdminTargetIsHidden();
        test.departmentRevocationRemovesEmployeeAuthorityOnNextResolution();
        test.adminCanCreateEmployeesByDefault();
    }
}
