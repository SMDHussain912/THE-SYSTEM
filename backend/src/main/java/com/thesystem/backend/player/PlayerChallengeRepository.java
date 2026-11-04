package com.thesystem.backend.player;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerChallengeRepository extends JpaRepository<PlayerChallenge, UUID> {
	List<PlayerChallenge> findByOnboarding_IdOrderByDisplayOrder(UUID onboardingId);

	@Modifying
	@Query("delete from PlayerChallenge challenge where challenge.onboarding.id = :onboardingId")
	void deleteByOnboardingId(@Param("onboardingId") UUID onboardingId);
}
