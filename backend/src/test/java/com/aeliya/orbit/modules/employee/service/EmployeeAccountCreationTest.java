package com.aeliya.orbit.modules.employee.service;

import com.aeliya.orbit.modules.employee.dto.EmployeeRequest;
import com.aeliya.orbit.modules.employee.entity.Employee;
import com.aeliya.orbit.modules.employee.repository.EmployeeRepository;
import com.aeliya.orbit.modules.employee.repository.OrganizationOptionRepository;
import com.aeliya.orbit.modules.permission.service.DepartmentModuleCache;
import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeAccountCreationTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final OrganizationOptionRepository options = mock(OrganizationOptionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DepartmentModuleCache cache = mock(DepartmentModuleCache.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EmployeeService service = new EmployeeService(employees, options, users, mock(JdbcTemplate.class), cache, encoder,
        mock(ProfileFileStore.class));
    private final User editor = new User("admin", "hash", "ROLE_ADMIN");

    private UsernamePasswordAuthenticationToken actor(String permission) {
        return new UsernamePasswordAuthenticationToken("admin", null, java.util.List.of(new SimpleGrantedAuthority(permission)));
    }

    private EmployeeRequest input(String email, String password) {
        return new EmployeeRequest("EMP-1", password, "New Employee", null, null, email, null,
            LocalDate.of(2026, 10, 22), null, null, null, null);
    }

    @Test void creatingEmployeeCreatesUserWithEmailAndHashedPassword() {
        ReflectionTestUtils.setField(editor, "id", 1L);
        when(users.findByUsername("admin")).thenReturn(Optional.of(editor));
        when(encoder.encode("long-password")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });
        when(employees.saveAndFlush(any(Employee.class))).thenAnswer(call -> {
            Employee saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });

        var result = service.create(input(" Person@Example.com ", "long-password"), actor("EMPLOYEE_CREATE"));
        assertEquals("person@example.com", result.workEmail());
        var account = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(account.capture());
        assertEquals("person@example.com", account.getValue().getUsername());
        assertEquals("bcrypt-hash", account.getValue().getPassword());
        assertEquals("ROLE_USER", account.getValue().getRole());
        verify(employees).saveAndFlush(any(Employee.class));
    }

    @Test void creationRequiresPasswordAndRejectsExistingUsername() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(editor));
        assertThrows(ResponseStatusException.class, () -> service.create(input("person@example.com", null), actor("EMPLOYEE_CREATE")));
        when(users.findByUsernameIgnoreCase("person@example.com")).thenReturn(Optional.of(new User("Person@Example.com", "hash", "ROLE_USER")));
        assertThrows(ResponseStatusException.class, () -> service.create(input("person@example.com", "long-password"), actor("EMPLOYEE_CREATE")));
        verify(users, never()).saveAndFlush(any());
        verify(employees, never()).saveAndFlush(any());
    }

    @Test void changingWorkEmailChangesLinkedUsernameWithoutChangingPassword() {
        User account = new User("old@example.com", "existing-hash", "ROLE_USER");
        ReflectionTestUtils.setField(account, "id", 2L);
        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 3L);
        employee.setEmployeeCode("EMP-1");
        employee.setAccount(account);
        when(employees.visibleById(3L)).thenReturn(Optional.of(employee));
        when(users.findByUsername("admin")).thenReturn(Optional.of(editor));
        when(employees.saveAndFlush(employee)).thenReturn(employee);

        service.update(3L, input("New@Example.com", null), actor("EMPLOYEE_UPDATE"));
        assertEquals("new@example.com", account.getUsername());
        assertEquals("existing-hash", account.getPassword());
        verifyNoInteractions(encoder);
    }

    public static void main(String[] args) {
        new EmployeeAccountCreationTest().creatingEmployeeCreatesUserWithEmailAndHashedPassword();
        new EmployeeAccountCreationTest().creationRequiresPasswordAndRejectsExistingUsername();
        new EmployeeAccountCreationTest().changingWorkEmailChangesLinkedUsernameWithoutChangingPassword();
    }
}
