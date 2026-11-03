package com.thesystem.backend.account;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "accounts", uniqueConstraints = {
		@UniqueConstraint(name = "uk_accounts_email", columnNames = "email"),
		@UniqueConstraint(name = "uk_accounts_google_subject", columnNames = "google_subject")
})
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 50)
	private String displayName;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(length = 100)
	private String passwordHash;

	@Column(name = "google_subject", length = 255)
	private String googleSubject;

	@Column(name = "totp_secret", length = 512)
	private String totpSecret;

	@Column(name = "totp_enabled", nullable = false)
	private boolean totpEnabled;

	protected Account() {
	}

	public Account(String displayName, String email, String passwordHash) {
		this.displayName = displayName;
		this.email = email;
		this.passwordHash = passwordHash;
		this.totpEnabled = false;
	}

	public UUID getId() { return id; }
	public String getDisplayName() { return displayName; }
	public String getEmail() { return email; }
	public String getPasswordHash() { return passwordHash; }
	public String getGoogleSubject() { return googleSubject; }
	public String getTotpSecret() { return totpSecret; }
	public boolean isTotpEnabled() { return totpEnabled; }

	public void linkGoogle(String subject) { this.googleSubject = subject; }
	public void setTotpSecret(String secret) { this.totpSecret = secret; }
	public void setTotpEnabled(boolean enabled) { this.totpEnabled = enabled; }
	public void clearTotp() { this.totpSecret = null; this.totpEnabled = false; }
}