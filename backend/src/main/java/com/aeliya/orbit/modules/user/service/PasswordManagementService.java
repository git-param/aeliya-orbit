package com.aeliya.orbit.modules.user.service;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordManagementService {
    public record AccountView(long id, String username, String role, boolean active) {}
    public record AccountPage(List<AccountView> content, long totalElements, int totalPages) {}
    public record PasswordInput(String password) {
        @Override public String toString() { return "PasswordInput[redacted]"; }
    }

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public PasswordManagementService(UserRepository users, JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.users = users;
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public AccountPage list(String search, int page, Authentication actor) {
        User editor = editor(actor);
        if (page < 0 || page > 10000) throw bad("Invalid page");
        String normalized = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100) throw bad("Search is too long");
        String pattern = "%" + normalized.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        boolean superAdmin = "ROLE_SUPER_ADMIN".equals(editor.getRole());
        String scope = superAdmin ? "1 = 1" : "(role = 'ROLE_USER' OR id = ?)";
        String filter = " FROM users WHERE " + scope + " AND lower(username) LIKE ? ESCAPE '\\'";
        long count = superAdmin
            ? jdbc.queryForObject("SELECT count(*)" + filter, Long.class, pattern)
            : jdbc.queryForObject("SELECT count(*)" + filter, Long.class, editor.getId(), pattern);
        String sql = "SELECT id, username, role, active" + filter + " ORDER BY username LIMIT 20 OFFSET ?";
        List<AccountView> rows = superAdmin
            ? jdbc.query(sql, (rs, row) -> new AccountView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)),
                pattern, page * 20)
            : jdbc.query(sql, (rs, row) -> new AccountView(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)),
                editor.getId(), pattern, page * 20);
        return new AccountPage(rows, count, (int) ((count + 19) / 20));
    }

    @Transactional
    public void change(long targetId, PasswordInput input, Authentication actor) {
        User editor = editor(actor);
        User target = users.findById(targetId).orElseThrow(this::hidden);
        if (!allowed(editor, target)) throw hidden();
        String password = input == null ? null : input.password();
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw bad("Password must be at least 8 characters and at most 72 bytes");
        target.setPassword(encoder.encode(password));
        users.saveAndFlush(target);
        jdbc.update("INSERT INTO password_change_audit(actor_user_id, target_user_id) VALUES (?, ?)", editor.getId(), target.getId());
    }

    private User editor(Authentication actor) {
        if (actor == null || actor.getAuthorities() == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        User user = users.findByUsername(actor.getName()).filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        boolean authority = actor.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(user.getRole()));
        if (!authority || !("ROLE_ADMIN".equals(user.getRole()) || "ROLE_SUPER_ADMIN".equals(user.getRole())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    private boolean allowed(User editor, User target) {
        if ("ROLE_ADMIN".equals(editor.getRole())) return "ROLE_USER".equals(target.getRole())
            || editor.getId().equals(target.getId());
        return true;
    }

    private ResponseStatusException bad(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    private ResponseStatusException hidden() { return new ResponseStatusException(HttpStatus.NOT_FOUND); }
}
