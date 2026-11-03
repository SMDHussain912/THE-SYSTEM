package com.thesystem.backend.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException() {
		super("An account with this email already exists.");
	}
}
