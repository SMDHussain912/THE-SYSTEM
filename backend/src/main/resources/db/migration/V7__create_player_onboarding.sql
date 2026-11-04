CREATE TABLE player_onboarding (
    account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    player_name VARCHAR(50) NOT NULL,
    main_challenge VARCHAR(200),
    goal VARCHAR(500),
    situation VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    current_chapter VARCHAR(24) NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    skipped_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_player_onboarding_status
        CHECK (status IN ('PROFILE_REQUIRED', 'READY', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'))
);

CREATE TABLE player_challenges (
    id UUID PRIMARY KEY,
    onboarding_id UUID NOT NULL REFERENCES player_onboarding(account_id) ON DELETE CASCADE,
    description VARCHAR(200) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT uk_player_challenge_order UNIQUE (onboarding_id, display_order),
    CONSTRAINT ck_player_challenge_order CHECK (display_order >= 0)
);

INSERT INTO player_onboarding (
    account_id, player_name, status, current_chapter, completed_at, created_at, updated_at, version
)
SELECT id, display_name, 'COMPLETED', 'AWAKENING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM accounts;
