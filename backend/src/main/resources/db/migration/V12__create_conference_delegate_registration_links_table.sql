CREATE TABLE conference_delegate_registration_links (
    id           UUID PRIMARY KEY,
    event_id     UUID NOT NULL REFERENCES events (id),
    created_by   UUID NOT NULL REFERENCES users (id),
    expires_at   TIMESTAMP WITH TIME ZONE,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_conference_delegate_registration_links_event_id ON conference_delegate_registration_links (event_id);
