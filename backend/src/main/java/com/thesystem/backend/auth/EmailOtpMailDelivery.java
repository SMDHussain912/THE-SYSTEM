package com.thesystem.backend.auth;

import java.util.UUID;

public interface EmailOtpMailDelivery {
	void deliver(UUID codeId, String recipient, String rawCode);
}
