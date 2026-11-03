package com.thesystem.backend.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.thesystem.backend.account.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

	private static final String EMAIL = "player@example.com";
	private static final String PASSWORD = "MyStrongPass123!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearAccounts() {
		accountRepository.deleteAll();
	}

	@Test
	void registrationCreatesHashedAccountAndAuthenticatedSession() throws Exception {
		MvcResult registration = mockMvc.perform(post("/api/auth/register")
						.with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"  Player One  ","email":"PLAYER@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.displayName").value("Player One"))
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andReturn();

		var account = accountRepository.findByEmail(EMAIL).orElseThrow();
		assertNotEquals(PASSWORD, account.getPasswordHash());
		assertTrue(passwordEncoder.matches(PASSWORD, account.getPasswordHash()));

		MockHttpSession session = (MockHttpSession) registration.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(account.getId().toString()));
	}

	@Test
	void registrationRejectsAnEmailThatAlreadyExists() throws Exception {
		registerAccount();

		mockMvc.perform(post("/api/auth/register")
						.with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Another Player","email":"PLAYER@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("An account with this email already exists."));
	}

	@Test
	void loginAuthenticatesExistingAccountAndRejectsWrongPassword() throws Exception {
		registerAccount();

		mockMvc.perform(post("/api/auth/login")
						.with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"PLAYER@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL));

		mockMvc.perform(post("/api/auth/login")
						.with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"incorrect-password"}
								""".formatted(EMAIL)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Email or password is incorrect."));
	}

	@Test
	void accountProfileRequiresAnAuthenticatedSession() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	private void registerAccount() {
		String normalizedEmail = EMAIL.toLowerCase(Locale.ROOT);
		accountRepository.save(new com.thesystem.backend.account.Account(
				"Player One",
				normalizedEmail,
				passwordEncoder.encode(PASSWORD)));
	}

	private RequestPostProcessor csrfRequest() {
		try {
			MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andReturn();
			String body = result.getResponse().getContentAsString();
			String token = jsonValue(body, "token");
			String headerName = jsonValue(body, "headerName");
			return request -> {
				request.setCookies(result.getResponse().getCookie("XSRF-TOKEN"));
				request.addHeader(headerName, token);
				return request;
			};
		} catch (Exception exception) {
			throw new IllegalStateException("Could not request a CSRF token for the test.", exception);
		}
	}

	private String jsonValue(String json, String name) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\":\"([^\"]+)\"").matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("CSRF response did not contain " + name + ".");
		}
		return matcher.group(1);
	}
}
