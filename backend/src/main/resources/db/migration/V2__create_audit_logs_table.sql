CREATE TABLE audit_logs (
    id                   UUID PRIMARY KEY,
    user_id              UUID REFERENCES users (id) ON DELETE SET NULL,
    username_attempted   VARCHAR(100),
    event_type           VARCHAR(30) NOT NULL,
    ip_address           VARCHAR(45),
    user_agent           VARCHAR(255),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_audit_logs_user_id ON audit_logs (user_id);
