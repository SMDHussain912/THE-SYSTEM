package com.thesystem.backend.player;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/player")
public class PlayerOnboardingController {
	private final PlayerOnboardingService onboardingService;

	public PlayerOnboardingController(PlayerOnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	@GetMapping("/onboarding")
	public PlayerOnboardingResponses.Onboarding getOnboarding(@AuthenticationPrincipal Jwt jwt) {
		return onboardingService.get(accountId(jwt));
	}

	@PutMapping("/profile")
	public PlayerOnboardingResponses.Onboarding updateProfile(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.ProfileUpdate request) {
		return onboardingService.updateProfile(accountId(jwt), request);
	}

	@PostMapping("/awakening/choices")
	public PlayerOnboardingResponses.Onboarding recordChoice(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.Choice request) {
		return onboardingService.recordChoice(accountId(jwt), request);
	}

	@PostMapping("/awakening/accept")
	public PlayerOnboardingResponses.Onboarding accept(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.Progress request) {
		return onboardingService.accept(accountId(jwt), request);
	}

	@PostMapping("/awakening/decline")
	public PlayerOnboardingResponses.Onboarding decline(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.Progress request) {
		return onboardingService.decline(accountId(jwt), request);
	}

	@PostMapping("/awakening/skip")
	public PlayerOnboardingResponses.Onboarding skip(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.Progress request) {
		return onboardingService.skip(accountId(jwt), request);
	}

	@PostMapping("/awakening/complete")
	public PlayerOnboardingResponses.Onboarding complete(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.Progress request) {
		return onboardingService.complete(accountId(jwt), request);
	}

	@PostMapping("/awakening/replay")
	public PlayerOnboardingResponses.Onboarding replay(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PlayerOnboardingRequests.Progress request) {
		return onboardingService.replay(accountId(jwt), request);
	}

	private UUID accountId(Jwt jwt) {
		String id = jwt.getClaimAsString("account_id");
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalStateException("Authenticated token does not contain a valid account id.", exception);
		}
	}
}
