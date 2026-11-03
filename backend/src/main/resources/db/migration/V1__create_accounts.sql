CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    display_name VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    CONSTRAINT uk_accounts_email UNIQUE (email)
);
