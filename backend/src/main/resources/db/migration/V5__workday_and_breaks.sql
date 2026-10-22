CREATE TABLE workday_settings (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    timezone VARCHAR(80) NOT NULL,
    work_start TIME NOT NULL,
    work_end TIME NOT NULL,
    break_allowance_minutes INTEGER NOT NULL CHECK (break_allowance_minutes >= 0),
    weekly_off_days VARCHAR(100) NOT NULL DEFAULT ''
);
INSERT INTO workday_settings(id, timezone, work_start, work_end, break_allowance_minutes, weekly_off_days)
VALUES (1, 'Asia/Kolkata', '09:30', '18:30', 60, '');

CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX holidays_upcoming_idx ON holidays(holiday_date) WHERE active = TRUE;

CREATE TABLE break_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    CONSTRAINT break_end_after_start CHECK (ended_at IS NULL OR ended_at > started_at)
);
CREATE UNIQUE INDEX break_one_open_per_employee_idx ON break_records(employee_id) WHERE ended_at IS NULL;
CREATE INDEX break_employee_start_idx ON break_records(employee_id, started_at DESC);
