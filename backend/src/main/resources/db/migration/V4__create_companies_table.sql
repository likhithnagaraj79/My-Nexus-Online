CREATE TABLE companies (
    id           UUID PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Case-insensitive uniqueness on name is not enforced here: H2 (used for tests) does not
-- support expression/functional indexes in this mode. Enforced in the Phase 3 service layer.
CREATE INDEX ix_companies_name ON companies (name);
