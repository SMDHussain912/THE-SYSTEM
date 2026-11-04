package com.thesystem.backend.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.core.task.TaskRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;

@Service
public class EmailOtpLoginService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailOtpLoginService.class);
	static final int MAX_ATTEMPTS = 5;
	static final int MAX_ACCOUNT_ATTEMPTS = 10;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
	private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
	private static final Duration ACCOUNT_FAILURE_WINDOW = Duration.ofHours(1);
	private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(15);
	private static final String REQUEST_MESSAGE =
			"If an eligible account exists, a sign-in code has been sent.";
	private static final String DUMMY_CODE = "000000";

	private final AccountRepository accounts;
	private final EmailOtpLoginCodeRepository codes;
	private final EmailOtpLoginAttemptLimitRepository attemptLimits;
	private final PasswordEncoder passwordEncoder;
	private final EmailOtpMailDelivery mailDelivery;
	private final EmailOtpCodeCleanupService cleanup;
	private final String dummyCodeHash;

	public EmailOtpLoginService(AccountRepository accounts, EmailOtpLoginCodeRepository codes,
			EmailOtpLoginAttemptLimitRepository attemptLimits, PasswordEncoder passwordEncoder,
			EmailOtpMailDelivery mailDelivery, EmailOtpCodeCleanupService cleanup) {
		this.accounts = accounts;
		this.codes = codes;
		this.attemptLimits = attemptLimits;
		this.passwordEncoder = passwordEncoder;
		this.mailDelivery = mailDelivery;
		this.cleanup = cleanup;
		this.dummyCodeHash = passwordEncoder.encode("otp-login-dummy-value");
	}

	@Transactional
	public void requestCode(String email) {
		String rawCode = "%06d".formatted(RANDOM.nextInt(1_000_000));
		String codeHash = passwordEncoder.encode(rawCode);
		Account account = accounts.findByEmailForUpdate(AccountService.normalizeEmail(email)).orElse(null);
		if (account == null || !account.isEmailVerified()) return;

		Instant now = Instant.now();
		EmailOtpLoginAttemptLimit attemptLimit = attemptLimits.findByAccountIdForUpdate(account.getId()).orElse(null);
		if (attemptLimit != null) {
			if (attemptLimit.isLockedAt(now)) return;
			attemptLimit.clearExpiredWindowOrLock(now, ACCOUNT_FAILURE_WINDOW);
		}

		EmailOtpLoginCode code = codes.findByAccountIdForUpdate(account.getId()).orElse(null);
		if (code != null && code.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now)) return;

		if (code == null) {
			code = new EmailOtpLoginCode(account, codeHash, now, now.plus(CODE_LIFETIME));
		} else {
			code.replaceCode(codeHash, now, now.plus(CODE_LIFETIME));
		}
		UUID codeId = codes.save(code).getId();
		String recipient = account.getEmail();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					mailDelivery.deliver(codeId, recipient, rawCode);
				} catch (TaskRejectedException exception) {
					cleanup.delete(codeId);
					LOGGER.warn("Email sign-in delivery queue is full; discarded code {}.", codeId);
				}
			}
		});
	}

	@Transactional(noRollbackFor = ResponseStatusException.class)
	public Account verify(String email, String rawCode) {
		if (rawCode == null || !rawCode.matches("\\d{6}")) {
			passwordEncoder.matches(DUMMY_CODE, dummyCodeHash);
			throw invalidCode();
		}

		Account account = accounts.findByEmailForUpdate(AccountService.normalizeEmail(email)).orElse(null);
		if (account == null || !account.isEmailVerified()) {
			passwordEncoder.matches(rawCode, dummyCodeHash);
			throw invalidCode();
		}

		Instant now = Instant.now();
		EmailOtpLoginAttemptLimit attemptLimit = attemptLimits.findByAccountIdForUpdate(account.getId()).orElse(null);
		if (attemptLimit != null && attemptLimit.isLockedAt(now)) {
			passwordEncoder.matches(rawCode, dummyCodeHash);
			throw invalidCode();
		}
		if (attemptLimit != null) attemptLimit.clearExpiredWindowOrLock(now, ACCOUNT_FAILURE_WINDOW);

		EmailOtpLoginCode code = codes.findByAccountIdForUpdate(account.getId()).orElse(null);
		if (code == null) {
			passwordEncoder.matches(rawCode, dummyCodeHash);
			throw invalidCode();
		}
		if (!code.isUsableAt(now)) {
			passwordEncoder.matches(rawCode, code.getCodeHash());
			throw invalidCode();
		}
		if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
			code.recordFailedAttempt();
			if (attemptLimit == null) attemptLimit = new EmailOtpLoginAttemptLimit(account, now);
			attemptLimit.recordFailure(now, MAX_ACCOUNT_ATTEMPTS, ACCOUNT_FAILURE_WINDOW, ACCOUNT_LOCK_DURATION);
			attemptLimits.save(attemptLimit);
			throw invalidCode();
		}

		code.markUsed(now);
		if (attemptLimit != null) {
			attemptLimit.resetFailures(now);
		}
		return account;
	}

	private ResponseStatusException invalidCode() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "The sign-in code is invalid or has expired.");
	}

	static String requestMessage() { return REQUEST_MESSAGE; }
}
