package com.aeliya.orbit.modules.permission.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DepartmentModuleCache {
    private final JdbcTemplate jdbc;
    private final Map<Long, Set<String>> departments = new ConcurrentHashMap<>();
    private final Map<Long, Long> accounts = new ConcurrentHashMap<>();
    private final Set<Long> unassigned = ConcurrentHashMap.newKeySet();
    private final Map<String, String> moduleCodes = new ConcurrentHashMap<>();

    public DepartmentModuleCache(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void initialize() {
        moduleCodes.clear();
        jdbc.query("SELECT id, code FROM modules", (org.springframework.jdbc.core.RowCallbackHandler) rs -> moduleCodes.put(rs.getString(1), rs.getString(2)));
        Map<Long, Set<String>> loaded = new HashMap<>();
        jdbc.query("SELECT d.id, m.id FROM departments d LEFT JOIN LATERAL jsonb_array_elements_text(d.permissions) m(id) ON true",
            rs -> { loaded.computeIfAbsent(rs.getLong(1), ignored -> new HashSet<>());
                if (rs.getString(2) != null) loaded.get(rs.getLong(1)).add(rs.getString(2)); });
        departments.clear();
        loaded.forEach((id, ids) -> departments.put(id, Set.copyOf(ids)));
        accounts.clear();
        unassigned.clear();
        jdbc.query("SELECT account_id, department_id FROM employees WHERE account_id IS NOT NULL AND active = true",
            rs -> { long account = rs.getLong(1); Long department = (Long) rs.getObject(2);
                if (department == null) unassigned.add(account); else accounts.put(account, department); });
    }

    public Set<String> forAccount(long accountId) {
        Long department = accounts.get(accountId);
        if (department == null && !unassigned.contains(accountId)) {
            synchronized (accounts) {
                department = accounts.get(accountId);
                if (department == null && !unassigned.contains(accountId)) {
                    department = jdbc.query("SELECT department_id FROM employees WHERE account_id = ? AND active = true",
                        rs -> rs.next() ? (Long) rs.getObject(1) : null, accountId);
                    if (department == null) unassigned.add(accountId); else accounts.put(accountId, department);
                }
            }
        }
        return department == null ? Set.of() : forDepartment(department);
    }

    public String permissionCode(String id) {
        String code = moduleCodes.get(id);
        if (code == null) {
            code = jdbc.query("SELECT code FROM modules WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, id);
            if (code != null) moduleCodes.put(id, code);
        }
        return code;
    }

    public Set<String> allModuleAuthorities() {
        if (moduleCodes.isEmpty()) jdbc.query("SELECT id, code FROM modules", (org.springframework.jdbc.core.RowCallbackHandler) rs -> moduleCodes.put(rs.getString(1), rs.getString(2)));
        return moduleCodes.keySet().stream().map(id -> "MODULE_" + id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<String> forDepartment(long id) {
        return departments.computeIfAbsent(id, key -> Set.copyOf(jdbc.query(
            "SELECT jsonb_array_elements_text(permissions) FROM departments WHERE id = ?",
            (rs, row) -> rs.getString(1), key)));
    }

    public void refreshDepartmentAfterCommit(long id) {
        afterCommit(() -> { synchronized (departments) {
            departments.remove(id);
            forDepartment(id);
        }});
    }

    public void refreshAccountAfterCommit(long accountId) {
        afterCommit(() -> { synchronized (accounts) {
            accounts.remove(accountId);
            unassigned.remove(accountId);
            Long department = jdbc.query("SELECT department_id FROM employees WHERE account_id = ? AND active = true",
                rs -> rs.next() ? (Long) rs.getObject(1) : null, accountId);
            if (department == null) unassigned.add(accountId); else accounts.put(accountId, department);
        }});
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        else action.run();
    }
}
