-- Event Days no longer require a calendar date: Admin no longer creates them individually
-- (they're auto-created as Day 1/2/3 alongside their Event), and the only consumer of an
-- EventDay id (Validator check-in scans) never reads the date, only the id. Existing rows in
-- an already-deployed database keep their historical date rather than erroring on migrate.
ALTER TABLE event_days ALTER COLUMN date DROP NOT NULL;
