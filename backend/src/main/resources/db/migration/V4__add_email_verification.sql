ALTER TABLE accounts
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- Existing accounts predate mandatory email verification. Preserve their access.
ALTER TABLE accounts
    ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE TABLE email_verification_codes (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    used_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_email_verification_code_hash UNIQUE (code_hash)
);

CREATE INDEX idx_email_verification_account_created
    ON email_verification_codes (account_id, created_at DESC);
