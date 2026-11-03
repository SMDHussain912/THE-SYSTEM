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
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;
	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;
	@Column(name = "used_at")
	private Instant usedAt;

	protected PasswordResetToken() {}

	public PasswordResetToken(Account account, String tokenHash, Instant expiresAt) {
		this.account = account;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public Account getAccount() { return account; }
	public boolean isUsableAt(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
	public void markUsed(Instant now) { this.usedAt = now; }
}