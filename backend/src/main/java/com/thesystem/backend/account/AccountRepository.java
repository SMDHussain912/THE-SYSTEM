package com.thesystem.backend.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByEmail(String email);
	Optional<Account> findByGoogleSubject(String googleSubject);
	boolean existsByEmail(String email);
}