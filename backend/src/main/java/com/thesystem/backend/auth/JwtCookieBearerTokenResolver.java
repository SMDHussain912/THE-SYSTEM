package com.thesystem.backend.auth;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtCookieBearerTokenResolver implements BearerTokenResolver {
	@Override
	public String resolve(HttpServletRequest request) {
		if (request.getCookies() == null) return null;
		for (Cookie cookie : request.getCookies()) {
			if (JwtService.ACCESS_COOKIE.equals(cookie.getName())
					&& StringUtils.hasText(cookie.getValue())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
