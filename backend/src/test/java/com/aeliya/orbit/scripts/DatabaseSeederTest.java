package com.aeliya.orbit.scripts;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DatabaseSeederTest {
    @Test void absentCredentialsDoNothingAndPartialCredentialsFailClosed() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DatabaseSeeder seeder = new DatabaseSeeder(users, encoder, jdbc);
        seeder.bootstrap(null, null);
        verifyNoInteractions(users, encoder, jdbc);
        assertThrows(IllegalStateException.class, () -> seeder.bootstrap("root", null));
        verifyNoInteractions(users, encoder, jdbc);
    }

    @Test void existingSuperAdminPreventsAnotherBootstrapAccount() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DatabaseSeeder seeder = new DatabaseSeeder(users, encoder, mock(JdbcTemplate.class));
        when(users.existsByRole("ROLE_SUPER_ADMIN")).thenReturn(true);
        seeder.bootstrap("another-root", "credential-from-deployment");
        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test void firstBootstrapEncodesPasswordAndNeverChangesExistingUsername() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DatabaseSeeder seeder = new DatabaseSeeder(users, encoder, mock(JdbcTemplate.class));
        when(users.findByUsername("root")).thenReturn(Optional.empty());
        when(encoder.encode("credential-from-deployment")).thenReturn("encoded");
        seeder.bootstrap("root", "credential-from-deployment");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("ROLE_SUPER_ADMIN", saved.getValue().getRole());
        assertEquals("encoded", saved.getValue().getPassword());
        assertNotEquals("credential-from-deployment", saved.getValue().getPassword());

        when(users.findByUsername("root")).thenReturn(Optional.of(new User("root", "hash", "ROLE_USER")));
        assertThrows(IllegalStateException.class, () -> seeder.bootstrap("root", "credential-from-deployment"));
        verify(users, times(1)).save(any());
    }

    public static void main(String[] args) {
        DatabaseSeederTest test = new DatabaseSeederTest();
        test.absentCredentialsDoNothingAndPartialCredentialsFailClosed();
        test.existingSuperAdminPreventsAnotherBootstrapAccount();
        test.firstBootstrapEncodesPasswordAndNeverChangesExistingUsername();
    }
}
