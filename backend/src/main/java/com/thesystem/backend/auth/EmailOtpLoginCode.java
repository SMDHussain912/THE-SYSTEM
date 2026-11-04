package com.thesystem.backend.auth;

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
@Table(name = "email_otp_login_codes")
public class EmailOtpLoginCode {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	private Account account;

	@Column(name = "code_hash", nullable = false, length = 100)
	private String codeHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "used_at")
	private Instant usedAt;

	protected EmailOtpLoginCode() {}

	public EmailOtpLoginCode(Account account, String codeHash, Instant createdAt, Instant expiresAt) {
		this.account = account;
		replaceCode(codeHash, createdAt, expiresAt);
	}

	public Account getAccount() { return account; }
	public UUID getId() { return id; }
	public String getCodeHash() { return codeHash; }
	public Instant getCreatedAt() { return createdAt; }

	public boolean isUsableAt(Instant now) {
		return usedAt == null && expiresAt.isAfter(now) && attempts < EmailOtpLoginService.MAX_ATTEMPTS;
	}

	public void replaceCode(String codeHash, Instant createdAt, Instant expiresAt) {
		this.codeHash = codeHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
		this.attempts = 0;
		this.usedAt = null;
	}

	public void recordFailedAttempt() { attempts++; }
	public void markUsed(Instant now) { usedAt = now; }
}
