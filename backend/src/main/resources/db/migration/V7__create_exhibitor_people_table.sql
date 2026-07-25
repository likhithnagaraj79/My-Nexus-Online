CREATE TABLE exhibitor_people (
    id                    UUID PRIMARY KEY,
    submission_id         UUID NOT NULL REFERENCES exhibitor_submissions (id) ON DELETE CASCADE,
    company_id            UUID NOT NULL REFERENCES companies (id),
    name                  VARCHAR(150) NOT NULL,
    designation           VARCHAR(150) NOT NULL,
    printed               BOOLEAN NOT NULL DEFAULT FALSE,
    printed_at            TIMESTAMP WITH TIME ZONE,
    printed_by            UUID REFERENCES users (id),
    issued                BOOLEAN NOT NULL DEFAULT FALSE,
    issued_at             TIMESTAMP WITH TIME ZONE,
    issued_by             UUID REFERENCES users (id),
    issued_phone_number   VARCHAR(15),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_exhibitor_people_submission_id ON exhibitor_people (submission_id);
CREATE INDEX ix_exhibitor_people_company_id ON exhibitor_people (company_id);
CREATE INDEX ix_exhibitor_people_printed ON exhibitor_people (printed);
CREATE INDEX ix_exhibitor_people_issued ON exhibitor_people (issued);
