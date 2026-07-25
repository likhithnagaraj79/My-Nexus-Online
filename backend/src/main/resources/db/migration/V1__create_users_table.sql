CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    username               VARCHAR(100) NOT NULL,
    email                  VARCHAR(255),
    password_hash          VARCHAR(255) NOT NULL,
    role                   VARCHAR(20) NOT NULL,
    aadhar_number          VARCHAR(12),
    phone_number           VARCHAR(15),
    totp_secret            VARCHAR(64),
    must_change_password   BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts  INT NOT NULL DEFAULT 0,
    account_locked         BOOLEAN NOT NULL DEFAULT FALSE,
    locked_at              TIMESTAMP WITH TIME ZONE,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id          UUID REFERENCES users (id),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_users_username ON users (username);
