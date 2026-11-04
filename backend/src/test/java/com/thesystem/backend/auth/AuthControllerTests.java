package com.thesystem.backend.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.mockito.ArgumentCaptor;

import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {
	private static final String EMAIL = "player@example.com";
	private static final String PASSWORD = "MyStrongPass123!";

	@Autowired private MockMvc mockMvc;
	@Autowired private AccountRepository accountRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@MockitoBean private JavaMailSender mailSender;

	@BeforeEach
	void clearAccounts() { accountRepository.deleteAll(); }

	@Test
	void registrationSendsVerificationCodeAndDoesNotAuthenticateBeforeVerification() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"  Player One  ","email":"PLAYER@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.message").value("A verification code has been sent to your email address."));

		Account account = accountRepository.findByEmail(EMAIL).orElseThrow();
		assertNotEquals(PASSWORD, account.getPasswordHash());
		assertTrue(passwordEncoder.matches(PASSWORD, account.getPasswordHash()));
		org.junit.jupiter.api.Assertions.assertFalse(account.isEmailVerified());

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mail.capture());
		assertTrue(mail.getValue().getText().contains("verification code"));
	}

	@Test
	void correctEmailCodeVerifiesAccountAndIssuesAuthenticationCookie() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Player One","email":"%s","password":"%s"}
								""".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isAccepted());

		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mail.capture());
		Matcher codeMatcher = Pattern.compile("(?s).*?(\\d{6}).*").matcher(mail.getValue().getText());
		assertTrue(codeMatcher.matches());
		String code = codeMatcher.group(1);

		mockMvc.perform(post("/api/auth/email/verify").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","code":"%s"}
								""".formatted(EMAIL, code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(result -> assertTrue(result.getResponse().getCookie("SYSTEM_ACCESS") != null));

		assertTrue(accountRepository.findByEmail(EMAIL).orElseThrow().isEmailVerified());
	}

	@Test
	void registrationRejectsAnEmailThatAlreadyExists() throws Exception {
		registerVerifiedAccount();
		mockMvc.perform(post("/api/auth/register").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Another Player","email":"PLAYER@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("An account with this email already exists."));
	}

	@Test
	void loginRejectsUnverifiedAccountAndAcceptsVerifiedAccount() throws Exception {
		Account account = new Account("Player One", EMAIL, passwordEncoder.encode(PASSWORD));
		accountRepository.save(account);
		mockMvc.perform(post("/api/auth/login").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Please verify your email address before signing in."));

		account.setEmailVerified(true);
		accountRepository.save(account);
		mockMvc.perform(post("/api/auth/login").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"PLAYER@example.com","password":"%s"}
								""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL));

		mockMvc.perform(post("/api/auth/login").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"incorrect-password"}
								""".formatted(EMAIL)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Email or password is incorrect."));
	}

	@Test
	void accountProfileRequiresAnAuthenticatedSession() throws Exception {
		mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
	}

	private void registerVerifiedAccount() {
		Account account = new Account("Player One", EMAIL.toLowerCase(Locale.ROOT), passwordEncoder.encode(PASSWORD));
		account.setEmailVerified(true);
		accountRepository.save(account);
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
		if (!matcher.find()) throw new IllegalStateException("CSRF response did not contain " + name + ".");
		return matcher.group(1);
	}
}
