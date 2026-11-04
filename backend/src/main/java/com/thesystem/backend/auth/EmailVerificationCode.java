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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode {
	@Id @GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;
	@Column(name = "code_hash", nullable = false, unique = true, length = 64)
	private String codeHash;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;
	@Column(nullable = false)
	private int attempts;
	@Column(name = "used_at")
	private Instant usedAt;

	protected EmailVerificationCode() {}
	public EmailVerificationCode(Account account, String codeHash, Instant createdAt, Instant expiresAt) {
		this.account = account; this.codeHash = codeHash; this.createdAt = createdAt;
		this.expiresAt = expiresAt; this.attempts = 0;
	}
	public Account getAccount() { return account; }
	public String getCodeHash() { return codeHash; }
	public Instant getCreatedAt() { return createdAt; }
	public boolean isUsableAt(Instant now) {
		return usedAt == null && expiresAt.isAfter(now) && attempts < 5;
	}
	public void recordFailedAttempt() { attempts++; }
	public void markUsed(Instant now) { usedAt = now; }
}