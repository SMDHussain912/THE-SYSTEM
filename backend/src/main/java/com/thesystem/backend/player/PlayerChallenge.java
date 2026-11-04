package com.thesystem.backend.player;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "player_challenges", uniqueConstraints = {
		@UniqueConstraint(name = "uk_player_challenge_order", columnNames = { "onboarding_id", "display_order" })
})
public class PlayerChallenge {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "onboarding_id", nullable = false)
	private PlayerOnboarding onboarding;

	@Column(name = "description", nullable = false, length = 200)
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	protected PlayerChallenge() {}

	public PlayerChallenge(PlayerOnboarding onboarding, String description, int displayOrder) {
		this.onboarding = onboarding;
		this.description = description;
		this.displayOrder = displayOrder;
	}

	public UUID getId() { return id; }
	public PlayerOnboarding getOnboarding() { return onboarding; }
	public String getDescription() { return description; }
	public int getDisplayOrder() { return displayOrder; }
}
