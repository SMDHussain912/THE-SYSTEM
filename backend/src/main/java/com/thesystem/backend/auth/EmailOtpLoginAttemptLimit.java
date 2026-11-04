package com.thesystem.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.thesystem.backend.account.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_otp_login_attempt_limits")
public class EmailOtpLoginAttemptLimit {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	private Account account;

	@Column(name = "window_started_at", nullable = false)
	private Instant windowStartedAt;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	protected EmailOtpLoginAttemptLimit() {}

	public EmailOtpLoginAttemptLimit(Account account, Instant now) {
		this.account = account;
		this.windowStartedAt = now;
	}

	public boolean isLockedAt(Instant now) {
		return lockedUntil != null && lockedUntil.isAfter(now);
	}

	public void clearExpiredWindowOrLock(Instant now, Duration window) {
		if ((lockedUntil != null && !lockedUntil.isAfter(now))
				|| !windowStartedAt.plus(window).isAfter(now)) {
			resetFailures(now);
		}
	}

	public void recordFailure(Instant now, int maximumAttempts, Duration window, Duration lockDuration) {
		if (!windowStartedAt.plus(window).isAfter(now)) {
			windowStartedAt = now;
			failedAttempts = 0;
			lockedUntil = null;
		}
		failedAttempts++;
		if (failedAttempts >= maximumAttempts) lockedUntil = now.plus(lockDuration);
	}

	public void resetFailures(Instant now) {
		windowStartedAt = now;
		failedAttempts = 0;
		lockedUntil = null;
	}
}
