package com.thesystem.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;

@Service
public class EmailVerificationService {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
	private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
	private final AccountRepository accounts;
	private final EmailVerificationCodeRepository codes;
	private final JavaMailSender mailSender;
	private final String mailFrom;

	public EmailVerificationService(AccountRepository accounts, EmailVerificationCodeRepository codes,
			JavaMailSender mailSender, @Value("${app.mail.from:no-reply@the-system.local}") String mailFrom) {
		this.accounts = accounts; this.codes = codes; this.mailSender = mailSender; this.mailFrom = mailFrom;
	}

	@Transactional
	public void sendRegistrationCode(Account account) {
		if (!account.isEmailVerified()) issueCode(account, false);
	}

	@Transactional
	public void resendCode(String email) {
		Account account = accounts.findByEmail(AccountService.normalizeEmail(email)).orElse(null);
		if (account == null || account.isEmailVerified() || account.getPasswordHash() == null) return;
		issueCode(account, true);
	}

	@Transactional(noRollbackFor = ResponseStatusException.class)
	public Account verify(String email, String rawCode) {
		String normalizedEmail = AccountService.normalizeEmail(email);
		if (rawCode == null || !rawCode.matches("\\d{6}")) throw invalidCode();
		Account account = accounts.findByEmail(normalizedEmail).orElseThrow(this::invalidCode);
		if (account.isEmailVerified()) return account;
		EmailVerificationCode code = codes.findTopByAccount_IdOrderByCreatedAtDesc(account.getId())
				.orElseThrow(this::invalidCode);
		Instant now = Instant.now();
		if (!code.isUsableAt(now)) throw invalidCode();
		if (!MessageDigest.isEqual(hash(rawCode).getBytes(StandardCharsets.US_ASCII),
				code.getCodeHash().getBytes(StandardCharsets.US_ASCII))) {
			code.recordFailedAttempt();
			throw invalidCode();
		}
		code.markUsed(now);
		account.setEmailVerified(true);
		return account;
	}

	private void issueCode(Account account, boolean enforceCooldown) {
		Instant now = Instant.now();
		EmailVerificationCode previous = codes.findTopByAccount_IdOrderByCreatedAtDesc(account.getId()).orElse(null);
		if (enforceCooldown && previous != null && previous.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now)) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Please wait before requesting another verification code.");
		}
		codes.deleteByAccount_Id(account.getId());
		String rawCode = "%06d".formatted(RANDOM.nextInt(1_000_000));
		codes.save(new EmailVerificationCode(account, hash(rawCode), now, now.plus(CODE_LIFETIME)));
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(mailFrom); message.setTo(account.getEmail());
		message.setSubject("Verify your THE SYSTEM account");
		message.setText("Your THE SYSTEM verification code is: " + rawCode
				+ "\n\nThis code expires in 10 minutes and can only be used once."
				+ "\nIf you did not create this account, you can ignore this email.");
		mailSender.send(message);
	}

	private ResponseStatusException invalidCode() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "The verification code is invalid or has expired.");
	}

	private static String hash(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Could not process verification code.", exception);
		}
	}
}