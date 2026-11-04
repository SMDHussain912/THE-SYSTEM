package com.thesystem.backend.player;

import java.time.Instant;
import java.util.UUID;

import com.thesystem.backend.account.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "player_onboarding")
public class PlayerOnboarding {
	@Id
	private UUID id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "player_name", nullable = false, length = 50)
	private String playerName;

	@Column(name = "main_challenge", length = 200)
	private String mainChallenge;

	@Column(name = "goal", length = 500)
	private String goal;

	@Column(name = "situation", length = 2000)
	private String situation;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OnboardingStatus status;

	@Column(name = "current_chapter", nullable = false, length = 24)
	private String currentChapter;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "declined_at")
	private Instant declinedAt;

	@Column(name = "skipped_at")
	private Instant skippedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected PlayerOnboarding() {}

	public PlayerOnboarding(Account account, Instant now) {
		this.account = account;
		this.playerName = account.getDisplayName();
		this.status = OnboardingStatus.PROFILE_REQUIRED;
		this.currentChapter = "INITIALIZATION";
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() { return id; }
	public Account getAccount() { return account; }
	public String getPlayerName() { return playerName; }
	public String getMainChallenge() { return mainChallenge; }
	public String getGoal() { return goal; }
	public String getSituation() { return situation; }
	public OnboardingStatus getStatus() { return status; }
	public String getCurrentChapter() { return currentChapter; }
	public Instant getAcceptedAt() { return acceptedAt; }
	public Instant getDeclinedAt() { return declinedAt; }
	public Instant getSkippedAt() { return skippedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }

	public void updateProfile(String playerName, String mainChallenge, String goal, String situation, Instant now) {
		this.playerName = playerName;
		this.mainChallenge = mainChallenge;
		this.goal = goal;
		this.situation = situation;
		this.updatedAt = now;
		if (status == OnboardingStatus.PROFILE_REQUIRED) status = OnboardingStatus.READY;
	}

	public void advanceTo(String chapter, Instant now) {
		this.currentChapter = chapter;
		this.status = OnboardingStatus.IN_PROGRESS;
		this.updatedAt = now;
	}

	public void markReady(Instant now) {
		this.status = OnboardingStatus.READY;
		this.updatedAt = now;
	}

	public void accept(Instant now) {
		this.acceptedAt = now;
		this.declinedAt = null;
		this.currentChapter = "AWAKENING";
		this.status = OnboardingStatus.IN_PROGRESS;
		this.updatedAt = now;
	}

	public void decline(Instant now) {
		this.declinedAt = now;
		this.updatedAt = now;
	}

	public void skip(Instant now) {
		this.skippedAt = now;
		this.completedAt = now;
		this.status = OnboardingStatus.SKIPPED;
		this.currentChapter = "AWAKENING";
		this.updatedAt = now;
	}

	public void complete(Instant now) {
		this.completedAt = now;
		this.status = OnboardingStatus.COMPLETED;
		this.currentChapter = "AWAKENING";
		this.updatedAt = now;
	}

	public void restart(Instant now) {
		this.acceptedAt = null;
		this.declinedAt = null;
		this.skippedAt = null;
		this.completedAt = null;
		this.currentChapter = "INITIALIZATION";
		this.status = mainChallenge == null || mainChallenge.isBlank() || goal == null || goal.isBlank()
				? OnboardingStatus.PROFILE_REQUIRED
				: OnboardingStatus.READY;
		this.updatedAt = now;
	}
}
