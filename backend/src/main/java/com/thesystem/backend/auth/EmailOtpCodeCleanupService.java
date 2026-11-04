package com.thesystem.backend.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOtpCodeCleanupService {
	private final EmailOtpLoginCodeRepository codes;

	public EmailOtpCodeCleanupService(EmailOtpLoginCodeRepository codes) {
		this.codes = codes;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void delete(UUID codeId) {
		codes.deleteById(codeId);
	}
}
