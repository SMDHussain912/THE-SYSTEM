ALTER TABLE accounts ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE accounts ADD COLUMN google_subject VARCHAR(255);
ALTER TABLE accounts ADD COLUMN totp_secret VARCHAR(512);
ALTER TABLE accounts ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uk_accounts_google_subject ON accounts (google_subject);
