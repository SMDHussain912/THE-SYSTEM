package com.thesystem.backend.auth;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class AsyncEmailOtpMailDelivery implements EmailOtpMailDelivery {
	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEmailOtpMailDelivery.class);
	private final JavaMailSender mailSender;
	private final EmailOtpCodeCleanupService cleanup;
	private final String mailFrom;

	public AsyncEmailOtpMailDelivery(JavaMailSender mailSender, EmailOtpCodeCleanupService cleanup,
			@Value("${app.mail.from:no-reply@the-system.local}") String mailFrom) {
		this.mailSender = mailSender;
		this.cleanup = cleanup;
		this.mailFrom = mailFrom;
	}

	@Override
	@Async("otpMailExecutor")
	@Transactional
	public void deliver(UUID codeId, String recipient, String rawCode) {
		try {
			mailSender.send(message(recipient, rawCode));
		} catch (MailException exception) {
			cleanup.delete(codeId);
			LOGGER.error("Could not deliver an email sign-in code ({}).", exception.getClass().getSimpleName());
		}
	}

	private SimpleMailMessage message(String recipient, String rawCode) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(mailFrom);
		message.setTo(recipient);
		message.setSubject("Your THE SYSTEM sign-in code");
		message.setText("Your THE SYSTEM sign-in code is: " + rawCode
				+ "\n\nThis code expires in 10 minutes and can only be used once."
				+ "\nIf you did not request this code, you can ignore this email.");
		return message;
	}
}
