CREATE TABLE events (
    id           UUID PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- "At most one active event" is not enforced here: H2 (used for tests) does not support
-- partial/filtered unique indexes with a WHERE predicate. Enforced in the Phase 3 service layer.
CREATE INDEX ix_events_active ON events (active);

CREATE TABLE event_days (
    id           UUID PRIMARY KEY,
    event_id     UUID NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    day_number   INT NOT NULL,
    date         DATE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_event_days_event_id_day_number ON event_days (event_id, day_number);
