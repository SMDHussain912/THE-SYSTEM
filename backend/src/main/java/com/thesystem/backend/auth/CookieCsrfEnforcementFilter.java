package com.thesystem.backend.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class CookieCsrfEnforcementFilter extends OncePerRequestFilter {
	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");
	private final CookieCsrfTokenRepository csrfTokenRepository;

	CookieCsrfEnforcementFilter(CookieCsrfTokenRepository csrfTokenRepository) {
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!SAFE_METHODS.contains(request.getMethod())) {
			CsrfToken token = csrfTokenRepository.loadToken(request);
			String submittedToken = token == null ? null : request.getHeader(token.getHeaderName());
			if (token == null || submittedToken == null || !MessageDigest.isEqual(
					token.getToken().getBytes(StandardCharsets.UTF_8),
					submittedToken.getBytes(StandardCharsets.UTF_8))) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}
}
