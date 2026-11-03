package com.thesystem.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;

@Service
public class PasswordResetService {
	private static final SecureRandom RANDOM = new SecureRandom();
	private final AccountRepository accounts;
	private final PasswordResetTokenRepository tokens;
	private final PasswordEncoder encoder;
	private final JavaMailSender mailSender;
	private final String frontendUrl;
	private final String mailFrom;

	public PasswordResetService(AccountRepository accounts, PasswordResetTokenRepository tokens,
			PasswordEncoder encoder, JavaMailSender mailSender,
			@Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
			@Value("${app.mail.from:no-reply@the-system.local}") String mailFrom) {
		this.accounts = accounts;
		this.tokens = tokens;
		this.encoder = encoder;
		this.mailSender = mailSender;
		this.frontendUrl = frontendUrl;
		this.mailFrom = mailFrom;
	}

	@Transactional
	public void requestReset(String email) {
		Account account = accounts.findByEmail(AccountService.normalizeEmail(email)).orElse(null);
		if (account == null || account.getPasswordHash() == null) return;

		tokens.deleteByAccount_Id(account.getId());
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		tokens.save(new PasswordResetToken(account, hash(raw), Instant.now().plus(15, ChronoUnit.MINUTES)));

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(mailFrom);
		message.setTo(account.getEmail());
		message.setSubject("Reset your THE SYSTEM password");
		message.setText("Use this link within 15 minutes to reset your password:\n\n"
				+ frontendUrl.replaceAll("/+$", "") + "/?resetToken=" + raw
				+ "\n\nIf you did not request this, ignore this email.");
		mailSender.send(message);
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		if (rawToken == null || rawToken.length() < 32 || rawToken.length() > 128) {
			throw invalidToken();
		}
		PasswordResetToken token = tokens.findByTokenHash(hash(rawToken)).orElseThrow(this::invalidToken);
		Instant now = Instant.now();
		if (!token.isUsableAt(now)) throw invalidToken();
		Account account = token.getAccount();
		if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 72 bytes or fewer.");
		}
		account.setPasswordHash(encoder.encode(newPassword));
		token.markUsed(now);
	}

	private IllegalArgumentException invalidToken() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired.");
	}

	private static String hash(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Could not process password reset token.", exception);
		}
	}
}
