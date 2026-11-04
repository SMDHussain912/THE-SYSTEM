package com.thesystem.backend.player;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class PlayerOnboardingRequests {
	private PlayerOnboardingRequests() {}

	public record ProfileUpdate(
			@NotBlank @Size(max = 50) String playerName,
			@NotBlank @Size(max = 200) String mainChallenge,
			@NotNull @Size(max = 5) List<@NotBlank @Size(max = 200) String> additionalChallenges,
			@NotBlank @Size(max = 500) String goal,
			@Size(max = 2000) String situation,
			@NotNull @PositiveOrZero Long version) {}

	public record Choice(
			@NotNull AwakeningChapter chapter,
			@NotNull AwakeningChoice choice,
			@NotNull @PositiveOrZero Long version) {}

	public record Progress(
			@NotNull @PositiveOrZero Long version) {}
}
