package com.aeliya.orbit.modules.dashboard;

import com.aeliya.orbit.modules.employee.service.ProfileFileStore;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.aeliya.orbit.modules.workday.WorkdayController;

@RestController
@RequestMapping("/api/dashboard")
public class BirthdayController {
    private final JdbcTemplate jdbc;
    private final WorkdayController workday;
    private final ProfileFileStore files;
    public BirthdayController(JdbcTemplate jdbc, WorkdayController workday, ProfileFileStore files) {
        this.jdbc = jdbc; this.workday = workday; this.files = files;
    }
    public record Birthday(long employeeId, String employeeName, String monthDay, boolean today, LocalDate nextOccurrence,
        String portraitUrl) {}

    @GetMapping("/birthdays")
    public List<Birthday> birthdays() {
        LocalDate today = LocalDate.now(ZoneId.of(workday.settings().timezone()));
        List<Birthday> candidates = jdbc.query(
            "SELECT e.id, e.full_name, EXTRACT(MONTH FROM e.date_of_birth)::int, EXTRACT(DAY FROM e.date_of_birth)::int " +
            "FROM employees e LEFT JOIN users u ON e.account_id = u.id " +
            "WHERE e.active = TRUE AND e.date_of_birth IS NOT NULL AND (u.id IS NULL OR u.role = 'ROLE_USER') " +
            "AND EXTRACT(MONTH FROM e.date_of_birth) = ? " +
            "AND EXTRACT(DAY FROM e.date_of_birth) >= ? ORDER BY EXTRACT(DAY FROM e.date_of_birth), e.full_name",
            (rs, row) -> {
                int month = rs.getInt(3), day = rs.getInt(4);
                LocalDate next = nextDate(today, month, day);
                long id = rs.getLong(1);
                return new Birthday(id, rs.getString(2), String.format("%02d-%02d", month, day),
                    today.equals(next), next, "/api/dashboard/birthdays/" + id + "/portrait");
            }, today.getMonthValue(), today.getDayOfMonth());
        return candidates.stream().filter(b -> !b.nextOccurrence().isAfter(today.withDayOfMonth(today.lengthOfMonth())))
            .sorted(Comparator.comparing(Birthday::nextOccurrence).thenComparing(Birthday::employeeName))
            .toList();
    }

    @GetMapping("/birthdays/{id}/portrait")
    public ResponseEntity<Resource> portrait(@PathVariable long id) throws IOException {
        if (birthdays().stream().noneMatch(b -> b.employeeId() == id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Long accountId = jdbc.query("SELECT account_id FROM employees WHERE id = ?",
            rs -> rs.next() ? (Long) rs.getObject(1) : null, id);
        if (accountId != null) {
            var stored = files.find(accountId, ProfileFileStore.Kind.PROFILE);
            if (stored != null) return ResponseEntity.ok().contentType(MediaType.parseMediaType(stored.contentType()))
                .header("Cache-Control", "private, no-store").body(new FileSystemResource(stored.path()));
        }
        var legacy = jdbc.query("SELECT content_type, image_data FROM employee_profile_images WHERE employee_id = ?",
            rs -> rs.next() ? new Object[] {rs.getString(1), rs.getBytes(2)} : null, id);
        if (legacy == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) legacy[0]))
            .header("Cache-Control", "private, no-store").body(new ByteArrayResource((byte[]) legacy[1]));
    }

    private LocalDate nextDate(LocalDate today, int month, int day) {
        for (int year = today.getYear(); year <= today.getYear() + 4; year++) {
            try {
                LocalDate date = LocalDate.of(year, month, day);
                if (!date.isBefore(today)) return date;
            } catch (DateTimeException ignored) { /* A leap-day birthday waits for the next leap year. */ }
        }
        return today.plusYears(4);
    }
}
