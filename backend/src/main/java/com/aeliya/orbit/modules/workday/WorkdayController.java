package com.aeliya.orbit.modules.workday;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workday")
public class WorkdayController {
    private final JdbcTemplate jdbc;
    public WorkdayController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Settings(String timezone, String workStart, String workEnd, int breakAllowanceMinutes, List<String> weeklyOffDays) {}
    public record Holiday(LocalDate date, String name) {}
    public record HolidayRequest(LocalDate date, String name) {}

    @GetMapping("/settings")
    public Settings settings() {
        return jdbc.queryForObject("SELECT timezone, work_start, work_end, break_allowance_minutes, weekly_off_days FROM workday_settings WHERE id = 1",
            (rs, row) -> new Settings(rs.getString(1), rs.getTime(2).toLocalTime().toString(),
                rs.getTime(3).toLocalTime().toString(), rs.getInt(4),
                rs.getString(5).isBlank() ? List.of() : Arrays.asList(rs.getString(5).split(","))));
    }

    @PutMapping("/settings")
    public Settings update(@RequestBody Settings input) {
        if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workday settings");
        LocalTime start;
        LocalTime end;
        List<String> days;
        try {
            ZoneId.of(input.timezone());
            start = LocalTime.parse(input.workStart());
            end = LocalTime.parse(input.workEnd());
            if (!start.isBefore(end) || input.breakAllowanceMinutes() < 0 || input.breakAllowanceMinutes() > 1440
                || input.weeklyOffDays() == null || input.weeklyOffDays().size() > 7) throw new IllegalArgumentException();
            days = input.weeklyOffDays().stream().map(day -> DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)).name()).distinct().toList();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workday settings");
        }
        jdbc.update("UPDATE workday_settings SET timezone = ?, work_start = ?, work_end = ?, break_allowance_minutes = ?, weekly_off_days = ? WHERE id = 1",
            input.timezone(), Time.valueOf(start), Time.valueOf(end), input.breakAllowanceMinutes(), String.join(",", days));
        return settings();
    }

    @GetMapping("/holidays")
    public List<Holiday> holidays(@RequestParam(required = false) LocalDate from, @RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 366) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        LocalDate start = from == null ? LocalDate.now(ZoneId.of(settings().timezone())) : from;
        return jdbc.query("SELECT holiday_date, name FROM holidays WHERE active = TRUE AND holiday_date >= ? AND holiday_date < ? ORDER BY holiday_date LIMIT 100",
            (rs, row) -> new Holiday(rs.getDate(1).toLocalDate(), rs.getString(2)), start, start.plusDays(days));
    }

    @PostMapping("/holidays")
    public Holiday holiday(@RequestBody HolidayRequest input) {
        if (input == null || input.date() == null || input.name() == null || input.name().isBlank() || input.name().length() > 160)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        jdbc.update("INSERT INTO holidays(holiday_date, name) VALUES (?, ?) ON CONFLICT (holiday_date) DO UPDATE SET name = excluded.name, active = TRUE",
            input.date(), input.name().trim());
        return new Holiday(input.date(), input.name().trim());
    }
}
