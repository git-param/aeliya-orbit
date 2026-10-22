ALTER TABLE employees
    ALTER COLUMN profile_focus_x TYPE INTEGER,
    ALTER COLUMN profile_focus_y TYPE INTEGER;

CREATE TABLE password_change_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES users(id),
    target_user_id BIGINT NOT NULL REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX password_change_audit_target_time_idx ON password_change_audit(target_user_id, changed_at DESC);
