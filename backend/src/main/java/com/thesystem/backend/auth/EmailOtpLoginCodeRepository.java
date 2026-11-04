package com.thesystem.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EmailOtpLoginCodeRepository extends JpaRepository<EmailOtpLoginCode, UUID> {
	Optional<EmailOtpLoginCode> findByAccount_Id(UUID accountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select code from EmailOtpLoginCode code where code.account.id = :accountId")
	Optional<EmailOtpLoginCode> findByAccountIdForUpdate(@Param("accountId") UUID accountId);
}
