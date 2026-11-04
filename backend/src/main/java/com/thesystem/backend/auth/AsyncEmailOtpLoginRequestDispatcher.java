package com.thesystem.backend.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class AsyncEmailOtpLoginRequestDispatcher implements EmailOtpLoginRequestDispatcher {
	private final EmailOtpLoginService emailOtpLoginService;

	public AsyncEmailOtpLoginRequestDispatcher(EmailOtpLoginService emailOtpLoginService) {
		this.emailOtpLoginService = emailOtpLoginService;
	}

	@Override
	@Async("otpRequestExecutor")
	public void requestCode(String email) {
		emailOtpLoginService.requestCode(email);
	}
}
