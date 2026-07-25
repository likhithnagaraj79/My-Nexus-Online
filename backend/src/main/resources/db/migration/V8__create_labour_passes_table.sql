CREATE TABLE labour_passes (
    id             UUID PRIMARY KEY,
    pass_type      VARCHAR(20) NOT NULL,
    pass_count     INT NOT NULL,
    phone_number   VARCHAR(15) NOT NULL,
    stall_number   VARCHAR(20),
    event_id       UUID NOT NULL REFERENCES events (id),
    issued_by      UUID NOT NULL REFERENCES users (id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_labour_passes_event_id_pass_type ON labour_passes (event_id, pass_type);
