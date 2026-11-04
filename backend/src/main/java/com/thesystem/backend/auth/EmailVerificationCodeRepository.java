package com.thesystem.backend.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<EmailVerificationCode> findTopByAccount_IdOrderByCreatedAtDesc(UUID accountId);
	void deleteByAccount_Id(UUID accountId);
}