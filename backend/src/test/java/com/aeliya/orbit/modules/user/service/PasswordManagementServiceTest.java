package com.aeliya.orbit.modules.user.service;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordManagementServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordManagementService service = new PasswordManagementService(users, jdbc, encoder);

    private User account(long id, String name, String role) {
        User user = new User(name, "old-hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
    private Authentication actor(User user) {
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null,
            List.of(new SimpleGrantedAuthority(user.getRole())));
    }

    @Test void adminCanResetEmployeePasswordWithAudit() {
        var admin = account(1, "admin", "ROLE_ADMIN");
        var employee = account(2, "employee@example.com", "ROLE_USER");
        when(users.findById(2L)).thenReturn(Optional.of(employee));
        when(encoder.encode("new-password")).thenReturn("new-hash");
        service.change(2, new PasswordManagementService.PasswordInput("new-password"), actor(admin));
        assertEquals("new-hash", employee.getPassword());
        verify(users).saveAndFlush(employee);
        verify(jdbc).update("INSERT INTO password_change_audit(actor_user_id, target_user_id) VALUES (?, ?)", 1L, 2L);
    }

    @Test void adminCannotResetAdminOrSuperAdmin() {
        var admin = account(1, "admin", "ROLE_ADMIN");
        when(users.findById(2L)).thenReturn(Optional.of(account(2, "peer", "ROLE_ADMIN")));
        when(users.findById(3L)).thenReturn(Optional.of(account(3, "root", "ROLE_SUPER_ADMIN")));
        var authentication = actor(admin);
        assertThrows(ResponseStatusException.class, () -> service.change(2, new PasswordManagementService.PasswordInput("new-password"), authentication));
        assertThrows(ResponseStatusException.class, () -> service.change(3, new PasswordManagementService.PasswordInput("new-password"), authentication));
        verifyNoInteractions(encoder, jdbc);
    }

    @Test void adminCanResetOwnPassword() {
        var admin = account(1, "admin", "ROLE_ADMIN");
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        service.change(1, new PasswordManagementService.PasswordInput("new-password"), actor(admin));
        verify(jdbc).update("INSERT INTO password_change_audit(actor_user_id, target_user_id) VALUES (?, ?)", 1L, 1L);
    }

    @Test void superAdminCanResetEveryRole() {
        var root = account(1, "root", "ROLE_SUPER_ADMIN");
        when(users.findById(2L)).thenReturn(Optional.of(account(2, "admin", "ROLE_ADMIN")));
        when(users.findById(1L)).thenReturn(Optional.of(root));
        when(users.findById(3L)).thenReturn(Optional.of(account(3, "other-root", "ROLE_SUPER_ADMIN")));
        var authentication = actor(root);
        service.change(2, new PasswordManagementService.PasswordInput("new-password"), authentication);
        service.change(1, new PasswordManagementService.PasswordInput("new-password"), authentication);
        service.change(3, new PasswordManagementService.PasswordInput("new-password"), authentication);
        verify(jdbc).update("INSERT INTO password_change_audit(actor_user_id, target_user_id) VALUES (?, ?)", 1L, 2L);
        verify(jdbc).update("INSERT INTO password_change_audit(actor_user_id, target_user_id) VALUES (?, ?)", 1L, 1L);
        verify(jdbc).update("INSERT INTO password_change_audit(actor_user_id, target_user_id) VALUES (?, ?)", 1L, 3L);
    }

    @Test void ordinaryUserCannotManagePasswords() {
        var user = account(1, "person", "ROLE_USER");
        assertThrows(ResponseStatusException.class, () -> service.list("", 0, actor(user)));
        verifyNoInteractions(jdbc, encoder);
    }

    @Test void adminAccountSearchIsRestrictedToEmployeeRole() {
        var admin = account(1, "admin", "ROLE_ADMIN");
        when(jdbc.queryForObject(contains("role = 'ROLE_USER'"), eq(Long.class), eq(1L), anyString())).thenReturn(0L);
        service.list("person", 0, actor(admin));
        verify(jdbc).queryForObject(contains("role = 'ROLE_USER'"), eq(Long.class), eq(1L), eq("%person%"));
    }

    @Test void passwordInputDoesNotPrintSecret() {
        assertFalse(new PasswordManagementService.PasswordInput("secret-value").toString().contains("secret-value"));
    }

    public static void main(String[] args) {
        new PasswordManagementServiceTest().adminCanResetEmployeePasswordWithAudit();
        new PasswordManagementServiceTest().adminCannotResetAdminOrSuperAdmin();
        new PasswordManagementServiceTest().adminCanResetOwnPassword();
        new PasswordManagementServiceTest().superAdminCanResetEveryRole();
        new PasswordManagementServiceTest().ordinaryUserCannotManagePasswords();
        new PasswordManagementServiceTest().adminAccountSearchIsRestrictedToEmployeeRole();
        new PasswordManagementServiceTest().passwordInputDoesNotPrintSecret();
    }
}
