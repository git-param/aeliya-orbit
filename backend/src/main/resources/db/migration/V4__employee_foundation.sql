CREATE TABLE organization_options (
    id BIGSERIAL PRIMARY KEY,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('DEPARTMENT', 'DESIGNATION', 'LEVEL')),
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (kind, name)
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(40) NOT NULL UNIQUE,
    account_id BIGINT UNIQUE REFERENCES users(id),
    full_name VARCHAR(160) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(30),
    work_email VARCHAR(255) NOT NULL UNIQUE,
    date_of_birth DATE,
    date_of_joining DATE NOT NULL,
    designation_id BIGINT REFERENCES organization_options(id),
    department_id BIGINT REFERENCES organization_options(id),
    level_id BIGINT REFERENCES organization_options(id),
    reporting_manager_id BIGINT REFERENCES employees(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    CONSTRAINT employee_not_self_manager CHECK (id <> reporting_manager_id)
);
CREATE INDEX employees_department_active_idx ON employees(department_id, active);
CREATE INDEX employees_reporting_manager_idx ON employees(reporting_manager_id);
CREATE INDEX employees_name_idx ON employees(full_name);

CREATE TABLE employee_profile_images (
    employee_id BIGINT PRIMARY KEY REFERENCES employees(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    image_data BYTEA NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
