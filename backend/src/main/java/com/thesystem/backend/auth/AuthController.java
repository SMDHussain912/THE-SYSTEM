package com.thesystem.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AccountService accountService;
	private final AccountRepository accountRepository;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final CsrfTokenRepository csrfTokenRepository;

	public AuthController(
			AccountService accountService,
			AccountRepository accountRepository,
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository) {
		this.accountService = accountService;
		this.accountRepository = accountRepository;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
	}

	@PostMapping("/register")
	public ResponseEntity<AccountProfile> register(
			@Valid @RequestBody AuthRequests.Register request,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		Account account = accountService.register(request);
		Authentication authentication = authenticate(account.getEmail(), request.password());
		saveAuthentication(authentication, servletRequest, servletResponse);
		return ResponseEntity.status(HttpStatus.CREATED).body(AccountProfile.from(account));
	}

	@PostMapping("/login")
	public AccountProfile login(
			@Valid @RequestBody AuthRequests.Login request,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		Authentication authentication = authenticate(
				AccountService.normalizeEmail(request.email()),
				request.password());
		saveAuthentication(authentication, servletRequest, servletResponse);
		return profileFor(authentication);
	}

	@GetMapping("/me")
	public AccountProfile me(Authentication authentication) {
		return profileFor(authentication);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		csrfTokenRepository.saveToken(null, request, response);
		SecurityContextHolder.clearContext();
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.noContent().build();
	}

	private Authentication authenticate(String email, String password) {
		return authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(email, password));
	}

	private void saveAuthentication(
			Authentication authentication,
			HttpServletRequest request,
			HttpServletResponse response) {
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}

	private AccountProfile profileFor(Authentication authentication) {
		String email = ((UserDetails) authentication.getPrincipal()).getUsername();
		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists."));
		return new AccountProfile(account.getId(), account.getDisplayName(), account.getEmail());
	}
}
