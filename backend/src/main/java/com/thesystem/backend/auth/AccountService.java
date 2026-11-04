package com.thesystem.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;
import com.thesystem.backend.player.PlayerOnboardingService;

@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final PlayerOnboardingService playerOnboardingService;

	public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
			PlayerOnboardingService playerOnboardingService) {
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.playerOnboardingService = playerOnboardingService;
	}

	@Transactional
	public Account register(AuthRequests.Register request) {
		String email = normalizeEmail(request.email());
		if (accountRepository.existsByEmail(email)) {
			throw new EmailAlreadyRegisteredException();
		}

		if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("Password must be 72 bytes or fewer.");
		}

		Account account = new Account(
				request.displayName().trim(),
				email,
				passwordEncoder.encode(request.password()));
		Account created = accountRepository.save(account);
		playerOnboardingService.initializeForNewAccount(created);
		return created;
	}

	@Transactional
	public Account resolveGoogleAccount(String subject, String email, String displayName) {
		String normalizedEmail = normalizeEmail(email);
		Account bySubject = accountRepository.findByGoogleSubject(subject).orElse(null);
		if (bySubject != null) return bySubject;

		Account account = accountRepository.findByEmail(normalizedEmail).orElse(null);
		if (account != null) {
			if (account.getGoogleSubject() != null && !account.getGoogleSubject().equals(subject)) {
				throw new IllegalArgumentException("This email is already linked to another Google account.");
			}
			account.linkGoogle(subject);
			account.setEmailVerified(true);
			return accountRepository.save(account);
		}

		Account created = new Account(
				displayName == null || displayName.isBlank() ? normalizedEmail : displayName.trim(),
				normalizedEmail,
				null);
		created.linkGoogle(subject);
		created.setEmailVerified(true);
		Account saved = accountRepository.save(created);
		playerOnboardingService.initializeForNewAccount(saved);
		return saved;
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
