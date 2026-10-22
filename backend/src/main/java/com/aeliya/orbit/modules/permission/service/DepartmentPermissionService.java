package com.aeliya.orbit.modules.permission.service;

import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepartmentPermissionService {
    public record ModuleView(String id, String name) {}
    public record DepartmentView(long id, String name, boolean active, long version, Set<String> moduleIds) {}
    public record UpdateRequest(long version, Set<String> moduleIds) {}

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final DepartmentModuleCache cache;

    public DepartmentPermissionService(JdbcTemplate jdbc, UserRepository users, DepartmentModuleCache cache) {
        this.jdbc = jdbc;
        this.users = users;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public List<ModuleView> modules(Authentication actor) {
        requireManager(actor);
        return jdbc.query("SELECT id, name FROM modules ORDER BY id",
            (rs, row) -> new ModuleView(rs.getString(1), rs.getString(2)));
    }

    @Transactional(readOnly = true)
    public List<DepartmentView> departments(Authentication actor) {
        requireManager(actor);
        return jdbc.query("SELECT d.id, o.name, o.active, d.version FROM departments d " +
                "JOIN organization_options o ON o.id = d.id AND o.kind = 'DEPARTMENT' ORDER BY o.name",
            (rs, row) -> new DepartmentView(rs.getLong(1), rs.getString(2), rs.getBoolean(3),
                rs.getLong(4), cache.forDepartment(rs.getLong(1))));
    }

    @Transactional
    public DepartmentView replace(long departmentId, UpdateRequest request, Authentication actor) {
        requireManager(actor);
        if (request == null || request.moduleIds() == null || request.moduleIds().contains(null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module IDs are required");
        Set<String> desired = new TreeSet<>(request.moduleIds());
        Set<String> known = new TreeSet<>(jdbc.queryForList("SELECT id FROM modules", String.class));
        if (!known.containsAll(desired)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown module ID");
        var rows = jdbc.query("SELECT d.version, d.permissions::text, o.name, o.active FROM departments d " +
                "JOIN organization_options o ON o.id = d.id AND o.kind = 'DEPARTMENT' WHERE d.id = ? FOR UPDATE OF d",
            (rs, row) -> new Object[] {rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)}, departmentId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Object[] current = rows.getFirst();
        long version = (long) current[0];
        if (request.version() != version) throw new ResponseStatusException(HttpStatus.CONFLICT, "Department permissions changed; reload and try again");
        String json = desired.stream().map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"));
        jdbc.update("UPDATE departments SET permissions = ?::jsonb, version = version + 1 WHERE id = ?", json, departmentId);
        long actorId = users.findByUsername(actor.getName()).filter(u -> u.isActive()).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.FORBIDDEN)).getId();
        jdbc.update("INSERT INTO department_permission_audit(department_id, actor_user_id, previous_permissions, new_permissions) " +
            "VALUES (?, ?, ?::jsonb, ?::jsonb)", departmentId, actorId, current[1], json);
        cache.refreshDepartmentAfterCommit(departmentId);
        return new DepartmentView(departmentId, (String) current[2], (boolean) current[3], version + 1, desired);
    }

    private void requireManager(Authentication actor) {
        if (actor == null || actor.getAuthorities() == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Set<String> authorities = actor.getAuthorities().stream().map(a -> a.getAuthority())
            .collect(java.util.stream.Collectors.toSet());
        if (!(authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPER_ADMIN"))
                || !authorities.contains("DEPARTMENT_MANAGE"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
