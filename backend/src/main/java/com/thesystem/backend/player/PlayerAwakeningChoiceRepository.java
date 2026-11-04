package com.thesystem.backend.player;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerAwakeningChoiceRepository extends JpaRepository<PlayerAwakeningChoice, UUID> {
	List<PlayerAwakeningChoice> findByOnboarding_IdOrderByCreatedAtAscIdAsc(UUID onboardingId);

	@Modifying
	@Query("delete from PlayerAwakeningChoice choice where choice.onboarding.id = :onboardingId")
	void deleteByOnboardingId(@Param("onboardingId") UUID onboardingId);
}
