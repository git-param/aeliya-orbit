package com.aeliya.orbit.modules.dashboard;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aeliya.orbit.modules.workday.WorkdayController;

@RestController
@RequestMapping("/api/dashboard")
public class BirthdayController {
    private final JdbcTemplate jdbc;
    private final WorkdayController workday;
    public BirthdayController(JdbcTemplate jdbc, WorkdayController workday) { this.jdbc = jdbc; this.workday = workday; }
    public record Birthday(String employeeName, String monthDay, boolean today, LocalDate nextOccurrence) {}

    @GetMapping("/birthdays")
    public List<Birthday> birthdays() {
        LocalDate today = LocalDate.now(ZoneId.of(workday.settings().timezone()));
        int nextMonth = today.getMonthValue() == 12 ? 1 : today.getMonthValue() + 1;
        List<Birthday> candidates = jdbc.query(
            "SELECT e.full_name, EXTRACT(MONTH FROM e.date_of_birth)::int, EXTRACT(DAY FROM e.date_of_birth)::int " +
            "FROM employees e LEFT JOIN users u ON e.account_id = u.id " +
            "WHERE e.active = TRUE AND e.date_of_birth IS NOT NULL AND (u.id IS NULL OR u.role = 'ROLE_USER') " +
            "AND EXTRACT(MONTH FROM e.date_of_birth) IN (?, ?) ORDER BY e.full_name LIMIT 500",
            (rs, row) -> {
                int month = rs.getInt(2), day = rs.getInt(3);
                LocalDate next = nextDate(today, month, day);
                return new Birthday(rs.getString(1), String.format("%02d-%02d", month, day),
                    today.equals(next), next);
            }, today.getMonthValue(), nextMonth);
        return candidates.stream().filter(b -> !b.nextOccurrence().isAfter(today.plusMonths(1)))
            .sorted(Comparator.comparing(Birthday::nextOccurrence).thenComparing(Birthday::employeeName))
            .limit(20).toList();
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
