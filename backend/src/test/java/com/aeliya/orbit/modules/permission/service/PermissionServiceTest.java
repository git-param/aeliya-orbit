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
    @Test void roleAndUserGrantsCombineButRoleIsNeverTakenFromClient() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PermissionService service = new PermissionService(jdbc, mock(UserRepository.class));
        User user = new User("employee", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 5L);
        when(jdbc.queryForList(contains("role_permission_grants"), eq(String.class), eq("ROLE_USER")))
            .thenReturn(List.of("EMPLOYEE_VIEW"));
        when(jdbc.queryForList(contains("user_permission_grants"), eq(String.class), eq(5L)))
            .thenReturn(List.of("LEAVE_APPROVE"));
        assertEquals(Set.of("ROLE_USER", "EMPLOYEE_VIEW", "LEAVE_APPROVE"), service.authoritiesFor(user));
    }

    @Test void nonSuperAdminCannotChangeGrantsAndSuperAdminTargetIsHidden() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UserRepository users = mock(UserRepository.class);
        PermissionService service = new PermissionService(jdbc, users);
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

    public static void main(String[] args) {
        PermissionServiceTest test = new PermissionServiceTest();
        test.roleAndUserGrantsCombineButRoleIsNeverTakenFromClient();
        test.nonSuperAdminCannotChangeGrantsAndSuperAdminTargetIsHidden();
    }
}
