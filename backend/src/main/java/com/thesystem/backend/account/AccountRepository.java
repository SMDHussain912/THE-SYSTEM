package com.thesystem.backend.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, UUID> {
	Optional<Account> findByEmail(String email);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select account from Account account where account.email = :email")
	Optional<Account> findByEmailForUpdate(@Param("email") String email);
	Optional<Account> findByGoogleSubject(String googleSubject);
	boolean existsByEmail(String email);
}