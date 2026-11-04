package com.thesystem.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EmailOtpLoginAttemptLimitRepository extends JpaRepository<EmailOtpLoginAttemptLimit, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select limitRecord from EmailOtpLoginAttemptLimit limitRecord where limitRecord.account.id = :accountId")
	Optional<EmailOtpLoginAttemptLimit> findByAccountIdForUpdate(@Param("accountId") UUID accountId);
}
