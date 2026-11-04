ALTER TABLE player_onboarding
    ADD COLUMN declined_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE player_awakening_choices (
    id UUID PRIMARY KEY,
    onboarding_id UUID NOT NULL REFERENCES player_onboarding(account_id) ON DELETE CASCADE,
    chapter VARCHAR(24) NOT NULL,
    choice VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_player_awakening_chapter
        CHECK (chapter IN ('INITIALIZATION', 'IDENTIFICATION', 'ENCOUNTER', 'DESPAIR')),
    CONSTRAINT ck_player_awakening_choice
        CHECK (choice IN ('CONTINUE', 'CONFRONT', 'ADAPT', 'OBSERVE'))
);

CREATE INDEX idx_player_awakening_choices_progress
    ON player_awakening_choices (onboarding_id, created_at, id);
