ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_USER'));

CREATE TABLE permissions (
    code VARCHAR(80) PRIMARY KEY
);
INSERT INTO permissions (code) VALUES
    ('EMPLOYEE_VIEW'), ('EMPLOYEE_CREATE'), ('EMPLOYEE_UPDATE'), ('EMPLOYEE_DEACTIVATE'),
    ('ATTENDANCE_MANAGE'), ('LEAVE_APPROVE'), ('PAYROLL_VIEW'), ('PAYROLL_MANAGE'),
    ('HOLIDAY_MANAGE'), ('DEPARTMENT_MANAGE'), ('PERMISSION_MANAGE');

CREATE TABLE role_permission_grants (
    role VARCHAR(32) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER')),
    permission_code VARCHAR(80) NOT NULL REFERENCES permissions(code),
    PRIMARY KEY (role, permission_code)
);
CREATE INDEX role_permission_grants_permission_idx ON role_permission_grants(permission_code);

CREATE TABLE user_permission_grants (
    user_id BIGINT NOT NULL REFERENCES users(id),
    permission_code VARCHAR(80) NOT NULL REFERENCES permissions(code),
    PRIMARY KEY (user_id, permission_code)
);
CREATE INDEX user_permission_grants_permission_idx ON user_permission_grants(permission_code);

CREATE TABLE permission_change_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES users(id),
    target_role VARCHAR(32),
    target_user_id BIGINT REFERENCES users(id),
    permission_code VARCHAR(80) NOT NULL REFERENCES permissions(code),
    action VARCHAR(8) NOT NULL CHECK (action IN ('GRANT', 'REVOKE')),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT permission_audit_target_check CHECK ((target_role IS NULL) <> (target_user_id IS NULL))
);
CREATE INDEX permission_change_audit_actor_time_idx ON permission_change_audit(actor_user_id, changed_at DESC);

INSERT INTO role_permission_grants (role, permission_code) VALUES
    ('ROLE_ADMIN', 'EMPLOYEE_VIEW'), ('ROLE_ADMIN', 'EMPLOYEE_CREATE'),
    ('ROLE_ADMIN', 'EMPLOYEE_UPDATE'), ('ROLE_ADMIN', 'EMPLOYEE_DEACTIVATE'),
    ('ROLE_ADMIN', 'ATTENDANCE_MANAGE'), ('ROLE_ADMIN', 'LEAVE_APPROVE'),
    ('ROLE_ADMIN', 'HOLIDAY_MANAGE'), ('ROLE_ADMIN', 'DEPARTMENT_MANAGE');
