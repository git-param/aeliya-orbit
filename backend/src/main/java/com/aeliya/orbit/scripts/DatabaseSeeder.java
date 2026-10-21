package com.aeliya.orbit.scripts;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public DatabaseSeeder(UserRepository users, PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.users = users;
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        bootstrap(System.getenv("BOOTSTRAP_SUPER_ADMIN_USERNAME"),
            System.getenv("BOOTSTRAP_SUPER_ADMIN_PASSWORD"));
    }

    void bootstrap(String username, String password) {
        if (username == null && password == null) {
            return;
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Both bootstrap credentials must be supplied");
        }
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext('aeliya-bootstrap-super-admin'))");
        if (users.existsByRole("ROLE_SUPER_ADMIN")) {
            return;
        }
        if (users.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Bootstrap username already belongs to an account");
        }
        users.save(new User(username, encoder.encode(password), "ROLE_SUPER_ADMIN"));
    }
}
