package com.thesystem.backend.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class SynchronousEmailOtpLoginRequestDispatcher implements EmailOtpLoginRequestDispatcher {
	private final EmailOtpLoginService emailOtpLoginService;

	public SynchronousEmailOtpLoginRequestDispatcher(EmailOtpLoginService emailOtpLoginService) {
		this.emailOtpLoginService = emailOtpLoginService;
	}

	@Override
	public void requestCode(String email) {
		emailOtpLoginService.requestCode(email);
	}
}
