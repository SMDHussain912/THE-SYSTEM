package com.thesystem.backend.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtOAuth2SuccessHandler implements AuthenticationSuccessHandler {
	private final AccountRepository accounts;
	private final JwtService jwtService;
	private final String frontendUrl;
	private final boolean secureCookies;

	public JwtOAuth2SuccessHandler(
			AccountRepository accounts,
			JwtService jwtService,
			@Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
			@Value("${app.security.cookie-secure:false}") boolean secureCookies) {
		this.accounts = accounts;
		this.jwtService = jwtService;
		this.frontendUrl = frontendUrl;
		this.secureCookies = secureCookies;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		OAuth2User principal = (OAuth2User) authentication.getPrincipal();
		String email = (String) principal.getAttributes().get("email");
		Account account = accounts.findByEmail(AccountService.normalizeEmail(email))
				.orElseThrow(() -> new IllegalStateException("Google account was not provisioned."));
		Cookie cookie = new Cookie(JwtService.ACCESS_COOKIE, jwtService.createAccessToken(account));
		cookie.setHttpOnly(true);
		cookie.setSecure(secureCookies);
		cookie.setPath("/");
		cookie.setMaxAge((int) jwtService.getExpirationSeconds());
		cookie.setAttribute("SameSite", "Lax");
		response.addCookie(cookie);
		response.sendRedirect(frontendUrl);
	}
}
