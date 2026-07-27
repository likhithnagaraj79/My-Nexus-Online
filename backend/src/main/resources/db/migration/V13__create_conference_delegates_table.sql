CREATE TABLE conference_delegates (
    id             UUID PRIMARY KEY,
    link_id        UUID NOT NULL REFERENCES conference_delegate_registration_links (id),
    name           VARCHAR(150) NOT NULL,
    company_name   VARCHAR(200) NOT NULL,
    designation    VARCHAR(150) NOT NULL,
    mobile_number  VARCHAR(15) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    printed        BOOLEAN NOT NULL DEFAULT FALSE,
    printed_at     TIMESTAMP WITH TIME ZONE,
    printed_by     UUID REFERENCES users (id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_conference_delegates_link_id ON conference_delegates (link_id);
CREATE INDEX ix_conference_delegates_printed ON conference_delegates (printed);
