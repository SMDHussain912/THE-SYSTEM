package com.thesystem.backend.player;

import java.time.Instant;
import java.util.List;

public final class PlayerOnboardingResponses {
	private PlayerOnboardingResponses() {}

	public record ChoiceEntry(String chapter, String choice, Instant createdAt) {}

	public record Onboarding(
			String playerName,
			String mainChallenge,
			List<String> additionalChallenges,
			String goal,
			String situation,
			OnboardingStatus status,
			AwakeningChapter currentChapter,
			Instant acceptedAt,
			Instant declinedAt,
			Instant skippedAt,
			Instant completedAt,
			long version,
			List<ChoiceEntry> choices) {}
}
