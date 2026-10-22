package com.aeliya.orbit.modules.permission.service;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PermissionService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public PermissionService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    public Set<String> authoritiesFor(User user) {
        Set<String> authorities = new HashSet<>();
        authorities.add(user.getRole());
        if ("ROLE_SUPER_ADMIN".equals(user.getRole())) {
            authorities.addAll(catalog());
        } else {
            authorities.addAll(jdbc.queryForList(
                "SELECT permission_code FROM role_permission_grants WHERE role = ?", String.class, user.getRole()));
            authorities.addAll(jdbc.queryForList(
                "SELECT permission_code FROM user_permission_grants WHERE user_id = ?", String.class, user.getId()));
        }
        return authorities;
    }

    public Set<String> catalog() {
        return new TreeSet<>(jdbc.queryForList("SELECT code FROM permissions ORDER BY code", String.class));
    }

    public Set<String> roleGrants(String role) {
        checkEditableRole(role);
        return new TreeSet<>(jdbc.queryForList(
            "SELECT permission_code FROM role_permission_grants WHERE role = ? ORDER BY permission_code", String.class, role));
    }

    public Set<String> userGrants(long userId) {
        editableUser(userId);
        return new TreeSet<>(jdbc.queryForList(
            "SELECT permission_code FROM user_permission_grants WHERE user_id = ? ORDER BY permission_code", String.class, userId));
    }

    @Transactional
    public Set<String> replaceRoleGrants(String role, Set<String> requested, Authentication actor) {
        checkEditor(actor);
        checkEditableRole(role);
        Set<String> desired = checkedPermissions(requested);
        // Serialize grant changes for this role, including an initially empty grant set.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", "permission-role:" + role);
        Set<String> current = roleGrants(role);
        User editor = actorUser(actor);
        for (String code : difference(current, desired)) {
            jdbc.update("DELETE FROM role_permission_grants WHERE role = ? AND permission_code = ?", role, code);
            audit(editor.getId(), role, null, code, "REVOKE");
        }
        for (String code : difference(desired, current)) {
            jdbc.update("INSERT INTO role_permission_grants(role, permission_code) VALUES (?, ?)", role, code);
            audit(editor.getId(), role, null, code, "GRANT");
        }
        return roleGrants(role);
    }

    @Transactional
    public Set<String> replaceUserGrants(long userId, Set<String> requested, Authentication actor) {
        checkEditor(actor);
        User target = editableUser(userId);
        Set<String> desired = checkedPermissions(requested);
        // Lock the target row so concurrent replacements cannot lose updates.
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, target.getId());
        Set<String> current = userGrants(userId);
        User editor = actorUser(actor);
        for (String code : difference(current, desired)) {
            jdbc.update("DELETE FROM user_permission_grants WHERE user_id = ? AND permission_code = ?", userId, code);
            audit(editor.getId(), null, userId, code, "REVOKE");
        }
        for (String code : difference(desired, current)) {
            jdbc.update("INSERT INTO user_permission_grants(user_id, permission_code) VALUES (?, ?)", userId, code);
            audit(editor.getId(), null, userId, code, "GRANT");
        }
        return userGrants(userId);
    }

    private Set<String> checkedPermissions(Set<String> requested) {
        if (requested == null || requested.contains(null) || requested.contains("PERMISSION_MANAGE")
                || !catalog().containsAll(requested)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown permission code");
        }
        return new TreeSet<>(requested);
    }

    private void checkEditableRole(String role) {
        if (!"ROLE_ADMIN".equals(role) && !"ROLE_USER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role is not editable");
        }
    }

    private User editableUser(long id) {
        User user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if ("ROLE_SUPER_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return user;
    }

    private void checkEditor(Authentication actor) {
        if (actor == null || actor.getAuthorities().stream().noneMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private User actorUser(Authentication actor) {
        return users.findByUsername(actor.getName())
            .filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.removeAll(right);
        return result;
    }

    private void audit(Long actorId, String role, Long userId, String code, String action) {
        jdbc.update("INSERT INTO permission_change_audit(actor_user_id, target_role, target_user_id, permission_code, action) VALUES (?, ?, ?, ?, ?)",
            actorId, role, userId, code, action);
    }
}
