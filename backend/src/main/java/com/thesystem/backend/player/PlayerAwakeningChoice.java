package com.thesystem.backend.player;

import java.time.Instant;
import java.util.UUID;

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
@Table(name = "player_awakening_choices")
public class PlayerAwakeningChoice {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "onboarding_id", nullable = false)
	private PlayerOnboarding onboarding;

	@Column(name = "chapter", nullable = false, length = 24)
	private String chapter;

	@Column(name = "choice", nullable = false, length = 16)
	private String choice;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PlayerAwakeningChoice() {}

	public PlayerAwakeningChoice(PlayerOnboarding onboarding, AwakeningChapter chapter,
			AwakeningChoice choice, Instant createdAt) {
		this.onboarding = onboarding;
		this.chapter = chapter.name();
		this.choice = choice.name();
		this.createdAt = createdAt;
	}

	public UUID getId() { return id; }
	public String getChapter() { return chapter; }
	public String getChoice() { return choice; }
	public Instant getCreatedAt() { return createdAt; }
}
