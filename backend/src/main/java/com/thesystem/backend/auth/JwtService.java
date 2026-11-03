package com.thesystem.backend.auth;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.thesystem.backend.account.Account;

@Service
public class JwtService {
	public static final String ACCESS_COOKIE = "SYSTEM_ACCESS";
	private final JwtEncoder encoder;
	private final String issuer;
	private final long expirationSeconds;

	public JwtService(
			JwtEncoder encoder,
			@Value("${app.security.jwt-issuer}") String issuer,
			@Value("${app.security.jwt-expiration}") long expirationSeconds) {
		this.encoder = encoder;
		this.issuer = issuer;
		this.expirationSeconds = expirationSeconds;
	}

	public String createAccessToken(Account account) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(now)
				.expiresAt(now.plusSeconds(expirationSeconds))
				.subject(account.getEmail())
				.claim("account_id", account.getId().toString())
				.claim("token_type", "access")
				.build();
		return encoder.encode(JwtEncoderParameters.from(
				org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}

	static SecretKey secretKey(String encodedSecret) {
		if (encodedSecret == null || encodedSecret.isBlank()) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET must be set to a Base64-encoded random secret of at least 32 bytes.");
		}
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(encodedSecret);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET must be valid Base64.", exception);
		}
		if (bytes.length < 32) {
			throw new IllegalStateException("APP_SECURITY_JWT_SECRET must decode to at least 32 bytes.");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}
}
