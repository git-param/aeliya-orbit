ALTER TABLE employees ADD COLUMN profile_focus_x SMALLINT NOT NULL DEFAULT 50 CHECK (profile_focus_x BETWEEN 0 AND 100);
ALTER TABLE employees ADD COLUMN profile_focus_y SMALLINT NOT NULL DEFAULT 50 CHECK (profile_focus_y BETWEEN 0 AND 100);

CREATE TABLE employee_secondary_phones (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    phone VARCHAR(30) NOT NULL,
    UNIQUE (employee_id, phone)
);
