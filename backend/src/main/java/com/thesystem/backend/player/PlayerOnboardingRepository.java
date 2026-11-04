package com.thesystem.backend.player;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlayerOnboardingRepository extends JpaRepository<PlayerOnboarding, UUID> {
	Optional<PlayerOnboarding> findByAccount_Id(UUID accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select onboarding from PlayerOnboarding onboarding where onboarding.account.id = :accountId")
	Optional<PlayerOnboarding> findByAccountIdForUpdate(@Param("accountId") UUID accountId);
}
