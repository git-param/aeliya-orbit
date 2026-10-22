package com.aeliya.orbit.modules.workday;

import com.aeliya.orbit.modules.employee.entity.Employee;
import com.aeliya.orbit.modules.employee.repository.EmployeeRepository;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Timestamp;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/breaks")
public class BreakController {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final WorkdayController workday;
    public BreakController(JdbcTemplate jdbc, UserRepository users, EmployeeRepository employees, WorkdayController workday) {
        this.jdbc = jdbc; this.users = users; this.employees = employees; this.workday = workday;
    }
    public record BreakStatus(long consumedSeconds, long remainingSeconds, int allowanceMinutes, Instant activeSince) {}

    @GetMapping("/today")
    public BreakStatus today(Authentication actor) { return status(employee(actor)); }

    @PostMapping("/start")
    @Transactional
    public BreakStatus start(Authentication actor) {
        Employee employee = employee(actor);
        lock(employee.getId());
        Long open = jdbc.query("SELECT id FROM break_records WHERE employee_id = ? AND ended_at IS NULL",
            rs -> rs.next() ? rs.getLong(1) : null, employee.getId());
        if (open != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Break already active");
        jdbc.update("INSERT INTO break_records(employee_id, started_at) VALUES (?, ?)", employee.getId(), Timestamp.from(Instant.now()));
        return status(employee);
    }

    @PostMapping("/end")
    @Transactional
    public BreakStatus end(Authentication actor) {
        Employee employee = employee(actor);
        lock(employee.getId());
        int changed = jdbc.update("UPDATE break_records SET ended_at = ? WHERE employee_id = ? AND ended_at IS NULL",
            Timestamp.from(Instant.now()), employee.getId());
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "No active break");
        return status(employee);
    }

    private void lock(Long id) { jdbc.queryForObject("SELECT id FROM employees WHERE id = ? FOR UPDATE", Long.class, id); }
    private Employee employee(Authentication actor) {
        Long userId = users.findByUsername(actor.getName()).filter(u -> u.isActive())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
        return employees.findByAccountId(userId).filter(Employee::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active employee profile is linked"));
    }
    private BreakStatus status(Employee employee) {
        WorkdayController.Settings settings = workday.settings();
        ZoneId zone = ZoneId.of(settings.timezone());
        Instant start = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant();
        Instant now = Instant.now();
        var records = jdbc.query("SELECT started_at, ended_at FROM break_records WHERE employee_id = ? AND started_at < ? AND (ended_at IS NULL OR ended_at > ?)",
            (rs, row) -> new Instant[] {rs.getTimestamp(1).toInstant(), rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toInstant()},
            employee.getId(), Timestamp.from(end), Timestamp.from(start));
        long consumed = 0;
        Instant active = null;
        for (Instant[] record : records) {
            if (record[1] == null) active = record[0];
            Instant countedStart = record[0].isBefore(start) ? start : record[0];
            Instant countedEnd = record[1] == null ? now : record[1];
            if (countedEnd.isAfter(end)) countedEnd = end;
            consumed += Math.max(0, Duration.between(countedStart, countedEnd).getSeconds());
        }
        return new BreakStatus(consumed, Math.max(0, settings.breakAllowanceMinutes() * 60L - consumed), settings.breakAllowanceMinutes(), active);
    }
}
