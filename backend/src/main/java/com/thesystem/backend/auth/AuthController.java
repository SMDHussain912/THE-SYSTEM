package com.thesystem.backend.auth;

import org.springframework.beans.factory.annotation.Value;
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
	private final AccountService accountService;
	private final AccountRepository accountRepository;
	private final AuthenticationManager authenticationManager;
	private final CsrfTokenRepository csrfTokenRepository;
	private final JwtService jwtService;
	private final boolean secureCookies;

	public AuthController(AccountService accountService, AccountRepository accountRepository,
			AuthenticationManager authenticationManager, CsrfTokenRepository csrfTokenRepository,
			JwtService jwtService, @Value("${app.security.cookie-secure:false}") boolean secureCookies) {
		this.accountService = accountService;
		this.accountRepository = accountRepository;
		this.authenticationManager = authenticationManager;
		this.csrfTokenRepository = csrfTokenRepository;
		this.jwtService = jwtService;
		this.secureCookies = secureCookies;
	}

	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
	}

	@PostMapping("/register")
	public ResponseEntity<AccountProfile> register(@Valid @RequestBody AuthRequests.Register request,
			HttpServletResponse response) {
		Account account = accountService.register(request);
		authenticateAndIssueCookie(account.getEmail(), request.password(), response);
		return ResponseEntity.status(HttpStatus.CREATED).body(AccountProfile.from(account));
	}

	@PostMapping("/login")
	public AccountProfile login(@Valid @RequestBody AuthRequests.Login request, HttpServletResponse response) {
		String email = AccountService.normalizeEmail(request.email());
		authenticateAndIssueCookie(email, request.password(), response);
		return accountRepository.findByEmail(email).map(AccountProfile::from)
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists."));
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
		authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
		Account account = accountRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists."));
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
