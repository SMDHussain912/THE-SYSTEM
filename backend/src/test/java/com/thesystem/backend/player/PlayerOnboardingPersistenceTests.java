package com.thesystem.backend.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;
import com.thesystem.backend.auth.AccountService;
import com.thesystem.backend.auth.AuthRequests;

@SpringBootTest
@ActiveProfiles("test")
class PlayerOnboardingPersistenceTests {
	@Autowired private AccountRepository accountRepository;
	@Autowired private PlayerOnboardingRepository onboardingRepository;
	@Autowired private AccountService accountService;

	@BeforeEach
	void clearAccounts() {
		accountRepository.deleteAll();
	}

	@Test
	void registrationCreatesIncompletePlayerOnboarding() {
		Account account = accountService.register(
				new AuthRequests.Register("New Player", "new-player@example.com", "LongEnoughPassword123!"));

		PlayerOnboarding onboarding = onboardingRepository.findByAccount_Id(account.getId()).orElseThrow();
		assertEquals(account.getId(), onboarding.getId());
		assertEquals("New Player", onboarding.getPlayerName());
		assertEquals(OnboardingStatus.PROFILE_REQUIRED, onboarding.getStatus());
		assertEquals("INITIALIZATION", onboarding.getCurrentChapter());
		assertNull(onboarding.getMainChallenge());
		assertNull(onboarding.getGoal());
		assertNull(onboarding.getSituation());
	}

	@Test
	void newGoogleAccountCreatesIncompletePlayerOnboarding() {
		Account account = accountService.resolveGoogleAccount(
				"google-player-subject", "google-player@example.com", "Google Player");

		PlayerOnboarding onboarding = onboardingRepository.findByAccount_Id(account.getId()).orElseThrow();
		assertEquals("Google Player", onboarding.getPlayerName());
		assertEquals(OnboardingStatus.PROFILE_REQUIRED, onboarding.getStatus());
		assertTrue(account.isEmailVerified());
	}

	@Test
	void linkingGoogleToExistingAccountDoesNotReplaceItsOnboarding() {
		Account account = accountService.register(
				new AuthRequests.Register("Existing Player", "existing-player@example.com", "LongEnoughPassword123!"));
		PlayerOnboarding original = onboardingRepository.findByAccount_Id(account.getId()).orElseThrow();

		Account linked = accountService.resolveGoogleAccount(
				"linked-google-subject", account.getEmail(), "Google Display Name");

		PlayerOnboarding afterLink = onboardingRepository.findByAccount_Id(linked.getId()).orElseThrow();
		assertEquals(original.getId(), afterLink.getId());
		assertEquals("Existing Player", afterLink.getPlayerName());
		assertEquals(OnboardingStatus.PROFILE_REQUIRED, afterLink.getStatus());
	}
}
