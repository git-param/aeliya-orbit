CREATE TABLE modules (
    id VARCHAR(3) PRIMARY KEY CHECK (id ~ '^[0-9]{3}$'),
    code VARCHAR(80) NOT NULL UNIQUE REFERENCES permissions(code),
    name VARCHAR(120) NOT NULL
);
INSERT INTO modules (id, code, name) VALUES
    ('101', 'EMPLOYEE_VIEW', 'View employees'),
    ('102', 'EMPLOYEE_CREATE', 'Create employees'),
    ('103', 'EMPLOYEE_UPDATE', 'Update employees'),
    ('104', 'EMPLOYEE_DEACTIVATE', 'Deactivate employees');

CREATE TABLE departments (
    id BIGINT PRIMARY KEY REFERENCES organization_options(id),
    permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT department_permissions_array CHECK (jsonb_typeof(permissions) = 'array')
);
INSERT INTO departments (id) SELECT id FROM organization_options WHERE kind = 'DEPARTMENT';

CREATE TABLE department_permission_audit (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    actor_user_id BIGINT NOT NULL REFERENCES users(id),
    previous_permissions JSONB NOT NULL,
    new_permissions JSONB NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX department_permission_audit_department_time_idx
    ON department_permission_audit(department_id, changed_at DESC);
