package com.thesystem.backend.auth;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleAccountOAuth2UserService extends DefaultOAuth2UserService {

	private final AccountService accountService;

	public GoogleAccountOAuth2UserService(AccountService accountService) {
		this.accountService = accountService;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
		OAuth2User googleUser = super.loadUser(request);
		Map<String, Object> attributes = googleUser.getAttributes();
		String subject = (String) attributes.get("sub");
		String email = (String) attributes.get("email");
		String name = (String) attributes.get("name");
		boolean verified = Boolean.TRUE.equals(attributes.get("email_verified"));

		if (subject == null || email == null || !verified) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("invalid_google_identity"),
					"Google did not provide a verified email identity.");
		}

		accountService.resolveGoogleAccount(subject, email, name);
		return new DefaultOAuth2User(googleUser.getAuthorities(), attributes, "sub");
	}
}