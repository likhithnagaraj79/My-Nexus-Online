CREATE TABLE check_in_scans (
    id                    UUID PRIMARY KEY,
    exhibitor_person_id   UUID NOT NULL REFERENCES exhibitor_people (id),
    event_day_id          UUID NOT NULL REFERENCES event_days (id),
    validator_id          UUID NOT NULL REFERENCES users (id),
    qr_payload            VARCHAR(500) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_check_in_scans_event_day_id_validator_id ON check_in_scans (event_day_id, validator_id);
