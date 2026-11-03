package com.thesystem.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

	private AuthRequests() {
	}

	public record Register(
			@NotBlank @Size(max = 50) String displayName,
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(min = 10, max = 72) String password) {
	}

	public record Login(
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(max = 72) String password) {
	}
}
