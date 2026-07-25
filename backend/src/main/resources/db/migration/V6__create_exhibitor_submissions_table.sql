CREATE TABLE exhibitor_submissions (
    id               UUID PRIMARY KEY,
    company_id       UUID NOT NULL REFERENCES companies (id),
    link_id          UUID NOT NULL REFERENCES exhibitor_registration_links (id),
    declared_count   INT NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_exhibitor_submissions_company_id ON exhibitor_submissions (company_id);
CREATE INDEX ix_exhibitor_submissions_link_id ON exhibitor_submissions (link_id);
