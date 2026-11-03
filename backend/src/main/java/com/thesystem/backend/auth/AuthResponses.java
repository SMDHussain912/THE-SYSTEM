package com.thesystem.backend.auth;

import java.util.UUID;

import com.thesystem.backend.account.Account;

public final class AuthResponses {

	private AuthResponses() {
	}

	public record AccountProfile(UUID id, String displayName, String email) {

		public static AccountProfile from(Account account) {
			return new AccountProfile(account.getId(), account.getDisplayName(), account.getEmail());
		}
	}

	public record CsrfTokenResponse(String token, String headerName) {
	}

	public record ApiError(String message) {
	}
}
