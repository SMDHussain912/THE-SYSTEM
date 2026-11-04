package com.thesystem.backend.player;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesystem.backend.account.Account;

@Service
public class PlayerOnboardingService {
	private final PlayerOnboardingRepository onboardingRepository;
	private final PlayerChallengeRepository challengeRepository;
	private final PlayerAwakeningChoiceRepository choiceRepository;

	public PlayerOnboardingService(PlayerOnboardingRepository onboardingRepository,
			PlayerChallengeRepository challengeRepository,
			PlayerAwakeningChoiceRepository choiceRepository) {
		this.onboardingRepository = onboardingRepository;
		this.challengeRepository = challengeRepository;
		this.choiceRepository = choiceRepository;
	}

	@Transactional
	public PlayerOnboarding initializeForNewAccount(Account account) {
		return onboardingRepository.save(new PlayerOnboarding(account, Instant.now()));
	}

	@Transactional(readOnly = true)
	public PlayerOnboardingResponses.Onboarding get(UUID accountId) {
		PlayerOnboarding onboarding = requireOnboarding(accountId);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding updateProfile(
			UUID accountId, PlayerOnboardingRequests.ProfileUpdate request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		requireVersion(onboarding, request.version());

		String playerName = request.playerName().trim();
		String mainChallenge = request.mainChallenge().trim();
		String goal = request.goal().trim();
		String situation = request.situation() == null ? null : request.situation().trim();
		List<String> additionalChallenges = request.additionalChallenges().stream()
				.map(String::trim)
				.toList();

		if (additionalChallenges.stream().anyMatch(String::isEmpty)) {
			throw badRequest("Additional challenges cannot be empty.");
		}
		if (additionalChallenges.stream().map(challenge -> challenge.toLowerCase(Locale.ROOT)).distinct().count()
				!= additionalChallenges.size()) {
			throw badRequest("Additional challenges must be unique.");
		}

		onboarding.updateProfile(playerName, mainChallenge, goal,
				situation == null || situation.isEmpty() ? null : situation, Instant.now());
		challengeRepository.deleteByOnboardingId(onboarding.getId());
		for (int index = 0; index < additionalChallenges.size(); index++) {
			challengeRepository.save(new PlayerChallenge(onboarding, additionalChallenges.get(index), index));
		}
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding recordChoice(
			UUID accountId, PlayerOnboardingRequests.Choice request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		requireVersion(onboarding, request.version());
		requireStatus(onboarding, OnboardingStatus.READY, OnboardingStatus.IN_PROGRESS);

		AwakeningChapter current = parseChapter(onboarding.getCurrentChapter());
		if (request.chapter() != current) throw conflict("The awakening chapter has changed.");
		validateChoice(current, request.choice());

		AwakeningChapter next = switch (current) {
			case INITIALIZATION -> AwakeningChapter.IDENTIFICATION;
			case IDENTIFICATION -> AwakeningChapter.ENCOUNTER;
			case ENCOUNTER -> AwakeningChapter.DESPAIR;
			case DESPAIR -> AwakeningChapter.SYSTEM_OFFER;
			case SYSTEM_OFFER, AWAKENING -> throw conflict("No choice can be recorded for this chapter.");
		};
		Instant now = Instant.now();
		choiceRepository.save(new PlayerAwakeningChoice(onboarding, current, request.choice(), now));
		onboarding.advanceTo(next.name(), now);
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding accept(UUID accountId, PlayerOnboardingRequests.Progress request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		if (onboarding.getAcceptedAt() != null
				&& onboarding.getStatus() == OnboardingStatus.IN_PROGRESS
				&& AwakeningChapter.AWAKENING.name().equals(onboarding.getCurrentChapter())) {
			return toResponse(onboarding);
		}
		requireVersion(onboarding, request.version());
		requireOffer(onboarding);
		onboarding.accept(Instant.now());
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding decline(UUID accountId, PlayerOnboardingRequests.Progress request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		requireVersion(onboarding, request.version());
		requireOffer(onboarding);
		onboarding.decline(Instant.now());
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding skip(UUID accountId, PlayerOnboardingRequests.Progress request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		requireVersion(onboarding, request.version());
		requireStatus(onboarding, OnboardingStatus.READY, OnboardingStatus.IN_PROGRESS);
		onboarding.skip(Instant.now());
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding complete(UUID accountId, PlayerOnboardingRequests.Progress request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		if (onboarding.getStatus() == OnboardingStatus.COMPLETED && onboarding.getAcceptedAt() != null) {
			return toResponse(onboarding);
		}
		requireVersion(onboarding, request.version());
		if (onboarding.getAcceptedAt() == null || onboarding.getStatus() != OnboardingStatus.IN_PROGRESS) {
			throw conflict("The awakening must be accepted before it can be completed.");
		}
		onboarding.complete(Instant.now());
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	@Transactional
	public PlayerOnboardingResponses.Onboarding replay(UUID accountId, PlayerOnboardingRequests.Progress request) {
		PlayerOnboarding onboarding = lockOnboarding(accountId);
		requireVersion(onboarding, request.version());
		requireStatus(onboarding, OnboardingStatus.COMPLETED, OnboardingStatus.SKIPPED);
		choiceRepository.deleteByOnboardingId(onboarding.getId());
		onboarding.restart(Instant.now());
		onboardingRepository.saveAndFlush(onboarding);
		return toResponse(onboarding);
	}

	private PlayerOnboardingResponses.Onboarding toResponse(PlayerOnboarding onboarding) {
		List<String> challenges = challengeRepository
				.findByOnboarding_IdOrderByDisplayOrder(onboarding.getId()).stream()
				.map(PlayerChallenge::getDescription)
				.toList();
		List<PlayerOnboardingResponses.ChoiceEntry> choices = choiceRepository
				.findByOnboarding_IdOrderByCreatedAtAscIdAsc(onboarding.getId()).stream()
				.map(choice -> new PlayerOnboardingResponses.ChoiceEntry(
						choice.getChapter(), choice.getChoice(), choice.getCreatedAt()))
				.toList();
		return new PlayerOnboardingResponses.Onboarding(
				onboarding.getPlayerName(),
				onboarding.getMainChallenge(),
				challenges,
				onboarding.getGoal(),
				onboarding.getSituation(),
				onboarding.getStatus(),
				parseChapter(onboarding.getCurrentChapter()),
				onboarding.getAcceptedAt(),
				onboarding.getDeclinedAt(),
				onboarding.getSkippedAt(),
				onboarding.getCompletedAt(),
				onboarding.getVersion(),
				choices);
	}

	private PlayerOnboarding requireOnboarding(UUID accountId) {
		return onboardingRepository.findByAccount_Id(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player onboarding was not found."));
	}

	private PlayerOnboarding lockOnboarding(UUID accountId) {
		return onboardingRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player onboarding was not found."));
	}

	private void requireVersion(PlayerOnboarding onboarding, long version) {
		if (onboarding.getVersion() != version) throw conflict("Player onboarding has changed. Refresh and try again.");
	}

	private void requireStatus(PlayerOnboarding onboarding, OnboardingStatus... allowed) {
		for (OnboardingStatus status : allowed) {
			if (onboarding.getStatus() == status) return;
		}
		throw conflict("This action is not available in the current onboarding state.");
	}

	private void requireOffer(PlayerOnboarding onboarding) {
		requireStatus(onboarding, OnboardingStatus.IN_PROGRESS);
		if (!AwakeningChapter.SYSTEM_OFFER.name().equals(onboarding.getCurrentChapter())
				|| onboarding.getAcceptedAt() != null) {
			throw conflict("The Player offer is not currently available.");
		}
	}

	private void requireProfile(PlayerOnboarding onboarding) {
		if (onboarding.getMainChallenge() == null || onboarding.getMainChallenge().isBlank()
				|| onboarding.getGoal() == null || onboarding.getGoal().isBlank()) {
			throw conflict("Complete the player profile before starting the awakening.");
		}
	}

	private void validateChoice(AwakeningChapter chapter, AwakeningChoice choice) {
		boolean valid = switch (chapter) {
			case INITIALIZATION, IDENTIFICATION, DESPAIR -> choice == AwakeningChoice.CONTINUE;
			case ENCOUNTER -> choice == AwakeningChoice.CONFRONT
					|| choice == AwakeningChoice.ADAPT || choice == AwakeningChoice.OBSERVE;
			case SYSTEM_OFFER, AWAKENING -> false;
		};
		if (!valid) throw badRequest("That choice is not available in the current chapter.");
	}

	private AwakeningChapter parseChapter(String chapter) {
		try {
			return AwakeningChapter.valueOf(chapter);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Stored awakening chapter is invalid.", exception);
		}
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	private ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}
}
