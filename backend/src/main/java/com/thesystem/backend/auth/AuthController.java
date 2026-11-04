package com.thesystem.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;
import com.thesystem.backend.auth.AuthResponses.AccountProfile;
import com.thesystem.backend.auth.AuthResponses.CsrfTokenResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
	private final AccountService accountService;
	private final AccountRepository accountRepository;
	private final AuthenticationManager authenticationManager;
	private final CsrfTokenRepository csrfTokenRepository;
	private final JwtService jwtService;
	private final PasswordResetService passwordResetService;
	private final EmailVerificationService emailVerificationService;
	private final EmailOtpLoginService emailOtpLoginService;
	private final EmailOtpLoginRequestDispatcher emailOtpLoginRequestDispatcher;
	private final boolean secureCookies;

	public AuthController(AccountService accountService, AccountRepository accountRepository,
			AuthenticationManager authenticationManager, CsrfTokenRepository csrfTokenRepository,
			JwtService jwtService, PasswordResetService passwordResetService,
			EmailVerificationService emailVerificationService, EmailOtpLoginService emailOtpLoginService,
			EmailOtpLoginRequestDispatcher emailOtpLoginRequestDispatcher,
			@Value("${app.security.cookie-secure:false}") boolean secureCookies) {
		this.accountService = accountService;
		this.accountRepository = accountRepository;
		this.authenticationManager = authenticationManager;
		this.csrfTokenRepository = csrfTokenRepository;
		this.jwtService = jwtService;
		this.passwordResetService = passwordResetService;
		this.emailVerificationService = emailVerificationService;
		this.emailOtpLoginService = emailOtpLoginService;
		this.emailOtpLoginRequestDispatcher = emailOtpLoginRequestDispatcher;
		this.secureCookies = secureCookies;
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponses.MessageResponse> register(@Valid @RequestBody AuthRequests.Register request) {
		Account account = accountService.register(request);
		emailVerificationService.sendRegistrationCode(account);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AuthResponses.MessageResponse(
				"A verification code has been sent to your email address."));
	}

	@PostMapping("/email/verify")
	public AccountProfile verifyEmail(@Valid @RequestBody AuthRequests.EmailVerification request,
			HttpServletResponse response) {
		Account account = emailVerificationService.verify(request.email(), request.code());
		authenticateAndIssueCookie(account.getEmail(), null, response);
		return AccountProfile.from(account);
	}

	@PostMapping("/email/resend")
	public ResponseEntity<AuthResponses.MessageResponse> resendVerification(
			@Valid @RequestBody AuthRequests.EmailVerificationResend request) {
		emailVerificationService.resendCode(request.email());
		return ResponseEntity.ok(new AuthResponses.MessageResponse(
				"If the account needs verification, a new code will be sent shortly."));
	}

	@PostMapping("/login")
	public AccountProfile login(@Valid @RequestBody AuthRequests.Login request, HttpServletResponse response) {
		String email = AccountService.normalizeEmail(request.email());
		authenticateAndIssueCookie(email, request.password(), response);
		Account signedIn = accountRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists."));
		return AccountProfile.from(signedIn);
	}

	@PostMapping("/email/otp/request")
	public ResponseEntity<AuthResponses.MessageResponse> requestEmailOtp(
			@Valid @RequestBody AuthRequests.EmailOtpRequest request) {
		try {
			emailOtpLoginRequestDispatcher.requestCode(request.email());
		} catch (TaskRejectedException exception) {
			// Keep queue saturation indistinguishable from other non-delivery cases.
			LOGGER.warn("Email sign-in request queue is full.");
		}
		return ResponseEntity.accepted().body(new AuthResponses.MessageResponse(EmailOtpLoginService.requestMessage()));
	}

	@PostMapping("/email/otp/verify")
	public AccountProfile verifyEmailOtp(@Valid @RequestBody AuthRequests.EmailOtpVerification request,
			HttpServletResponse response) {
		Account account = emailOtpLoginService.verify(request.email(), request.code());
		issueAuthenticationCookie(account, response);
		return AccountProfile.from(account);
	}

	@PostMapping("/password/forgot")
	public ResponseEntity<AuthResponses.MessageResponse> requestPasswordReset(
			@Valid @RequestBody AuthRequests.PasswordResetRequest request) {
		passwordResetService.requestReset(request.email());
		return ResponseEntity.ok(new AuthResponses.MessageResponse(
				"If an account with that email exists, a password reset link has been sent."));
	}

	@PostMapping("/password/reset")
	public ResponseEntity<AuthResponses.MessageResponse> resetPassword(
			@Valid @RequestBody AuthRequests.PasswordResetConfirmation request) {
		passwordResetService.resetPassword(request.token(), request.newPassword());
		return ResponseEntity.ok(new AuthResponses.MessageResponse(
				"Your password has been updated. You can now sign in."));
	}

	@GetMapping("/me")
	public AccountProfile me(Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new IllegalStateException("Unexpected authentication principal.");
		}
		Account account = accountRepository.findByEmail(jwt.getSubject())
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists."));
		return AccountProfile.from(account);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		clearCookie(response, JwtService.ACCESS_COOKIE);
		csrfTokenRepository.saveToken(null, request, response);
		return ResponseEntity.noContent().build();
	}

	private void authenticateAndIssueCookie(String email, String password, HttpServletResponse response) {
		if (password != null) {
			authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
		}
		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists."));
		if (password != null && !account.isEmailVerified()) {
			throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
					"Please verify your email address before signing in.");
		}
		issueAuthenticationCookie(account, response);
	}

	private void issueAuthenticationCookie(Account account, HttpServletResponse response) {
		Cookie cookie = new Cookie(JwtService.ACCESS_COOKIE, jwtService.createAccessToken(account));
		cookie.setHttpOnly(true);
		cookie.setSecure(secureCookies);
		cookie.setPath("/");
		cookie.setMaxAge((int) jwtService.getExpirationSeconds());
		cookie.setAttribute("SameSite", "Lax");
		response.addCookie(cookie);
	}

	private void clearCookie(HttpServletResponse response, String name) {
		Cookie cookie = new Cookie(name, "");
		cookie.setHttpOnly(true);
		cookie.setSecure(secureCookies);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		cookie.setAttribute("SameSite", "Lax");
		response.addCookie(cookie);
	}
}
