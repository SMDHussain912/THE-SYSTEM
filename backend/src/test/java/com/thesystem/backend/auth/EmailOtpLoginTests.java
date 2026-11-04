package com.thesystem.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
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
class EmailOtpLoginTests {
	private static final String EMAIL = "otp-player@example.com";
	private static final String REQUEST_MESSAGE = "If an eligible account exists, a sign-in code has been sent.";
	private static final String INVALID_MESSAGE = "The sign-in code is invalid or has expired.";

	@Autowired private MockMvc mockMvc;
	@Autowired private AccountRepository accountRepository;
	@Autowired private EmailOtpLoginCodeRepository codeRepository;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private JdbcTemplate jdbcTemplate;
	@MockitoBean private JavaMailSender mailSender;

	@BeforeEach
	void resetTestData() {
		accountRepository.deleteAll();
		reset(mailSender);
	}

	@Test
	void successfulOtpLoginStoresOnlyHashAndIssuesSecureAuthenticationCookie() throws Exception {
		Account account = verifiedAccount();
		String code = requestCode(EMAIL);
		EmailOtpLoginCode storedCode = codeRepository.findByAccount_Id(account.getId()).orElseThrow();
		assertNotEquals(code, storedCode.getCodeHash());
		assertTrue(passwordEncoder.matches(code, storedCode.getCodeHash()));

		MvcResult result = mockMvc.perform(post("/api/auth/email/otp/verify").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content(verificationRequest(EMAIL, code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andReturn();

		var cookie = result.getResponse().getCookie("SYSTEM_ACCESS");
		assertTrue(cookie != null && cookie.isHttpOnly());
		assertFalse(cookie.getSecure());
		assertEquals("/", cookie.getPath());
		assertEquals("Lax", cookie.getAttribute("SameSite"));
		assertEquals(900, cookie.getMaxAge());
		assertEquals(400, verifyCode(EMAIL, code).getResponse().getStatus());
	}

	@Test
	void invalidOtpIsRejected() throws Exception {
		verifiedAccount();
		String code = requestCode(EMAIL);
		String wrongCode = code.equals("000000") ? "000001" : "000000";

		mockMvc.perform(post("/api/auth/email/otp/verify").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content(verificationRequest(EMAIL, wrongCode)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
	}

	@Test
	void expiredOtpIsRejected() throws Exception {
		Account account = verifiedAccount();
		String code = requestCode(EMAIL);
		jdbcTemplate.update("UPDATE email_otp_login_codes SET expires_at = ? WHERE account_id = ?",
				Timestamp.from(Instant.now().minusSeconds(1)), account.getId());

		mockMvc.perform(post("/api/auth/email/otp/verify").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content(verificationRequest(EMAIL, code)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
	}

	@Test
	void unverifiedAccountCannotUseOtpLogin() throws Exception {
		accountRepository.save(new Account("Unverified", EMAIL, null));
		requestOtp(EMAIL).andExpect(status().isAccepted())
				.andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));
		verify(mailSender, times(0)).send(any(SimpleMailMessage.class));

		mockMvc.perform(post("/api/auth/email/otp/verify").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content(verificationRequest(EMAIL, "123456")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
	}

	@Test
	void unknownEmailGetsSameGenericOtpRequestResponse() throws Exception {
		verifiedAccount();
		String existingResponse = requestOtp(EMAIL).andExpect(status().isAccepted()).andReturn()
				.getResponse().getContentAsString();
		String unknownResponse = requestOtp("unknown@example.com").andExpect(status().isAccepted()).andReturn()
				.getResponse().getContentAsString();

		assertEquals(existingResponse, unknownResponse);
		assertTrue(existingResponse.contains(REQUEST_MESSAGE));
		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	@Test
	void mailDeliveryFailureDoesNotRevealWhetherEmailExists() throws Exception {
		verifiedAccount();
		doThrow(new MailSendException("mail service unavailable"))
				.when(mailSender).send(any(SimpleMailMessage.class));

		requestOtp(EMAIL).andExpect(status().isAccepted())
				.andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));
		requestOtp("unknown@example.com").andExpect(status().isAccepted())
				.andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));
		assertTrue(codeRepository.findByAccount_Id(
				accountRepository.findByEmail(EMAIL).orElseThrow().getId()).isEmpty());
	}

	@Test
	void resendCooldownDoesNotSendAnotherCode() throws Exception {
		verifiedAccount();
		requestOtp(EMAIL).andExpect(status().isAccepted());
		requestOtp(EMAIL).andExpect(status().isAccepted())
				.andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));

		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	@Test
	void maximumAttemptsInvalidateTheCode() throws Exception {
		verifiedAccount();
		String code = requestCode(EMAIL);
		String wrongCode = code.equals("000000") ? "000001" : "000000";

		for (int attempt = 0; attempt < EmailOtpLoginService.MAX_ATTEMPTS; attempt++) {
			assertEquals(400, verifyCode(EMAIL, wrongCode).getResponse().getStatus());
		}

		mockMvc.perform(post("/api/auth/email/otp/verify").with(csrfRequest())
						.contentType(MediaType.APPLICATION_JSON)
						.content(verificationRequest(EMAIL, code)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_MESSAGE));
	}

	@Test
	void concurrentVerificationCanOnlyIssueOneAuthenticationCookie() throws Exception {
		verifiedAccount();
		String code = requestCode(EMAIL);
		RequestPostProcessor firstCsrf = csrfRequest();
		RequestPostProcessor secondCsrf = csrfRequest();

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<Integer> first = executor.submit(() -> verifyCode(EMAIL, code, firstCsrf).getResponse().getStatus());
			Future<Integer> second = executor.submit(() -> verifyCode(EMAIL, code, secondCsrf).getResponse().getStatus());
			int firstStatus = first.get();
			int secondStatus = second.get();

			assertTrue((firstStatus == 200 && secondStatus == 400)
					|| (firstStatus == 400 && secondStatus == 200));
		}
	}

	@Test
	void otpRequestAndVerificationRemainProtectedByCsrf() throws Exception {
		verifiedAccount();
		mockMvc.perform(post("/api/auth/email/otp/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\"}".formatted(EMAIL)))
				.andExpect(status().isForbidden());
	}

	private Account verifiedAccount() {
		Account account = new Account("OTP Player", EMAIL, null);
		account.setEmailVerified(true);
		return accountRepository.save(account);
	}

	private String requestCode(String email) throws Exception {
		requestOtp(email).andExpect(status().isAccepted());
		ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(mail.capture());
		Matcher matcher = Pattern.compile("(?s).*sign-in code is: (\\d{6}).*")
				.matcher(mail.getValue().getText());
		assertTrue(matcher.matches());
		return matcher.group(1);
	}

	private org.springframework.test.web.servlet.ResultActions requestOtp(String email) throws Exception {
		return mockMvc.perform(post("/api/auth/email/otp/request").with(csrfRequest())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\"}".formatted(email)));
	}

	private MvcResult verifyCode(String email, String code) throws Exception {
		return verifyCode(email, code, csrfRequest());
	}

	private MvcResult verifyCode(String email, String code, RequestPostProcessor csrf) throws Exception {
		return mockMvc.perform(post("/api/auth/email/otp/verify").with(csrf)
						.contentType(MediaType.APPLICATION_JSON)
						.content(verificationRequest(email, code)))
				.andReturn();
	}

	private String verificationRequest(String email, String code) {
		return """
				{"email":"%s","code":"%s"}
				""".formatted(email, code);
	}

	private RequestPostProcessor csrfRequest() {
		try {
			MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andReturn();
			String body = result.getResponse().getContentAsString();
			Matcher tokenMatcher = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(body);
			Matcher headerMatcher = Pattern.compile("\"headerName\":\"([^\"]+)\"").matcher(body);
			assertTrue(tokenMatcher.find() && headerMatcher.find());
			String token = tokenMatcher.group(1);
			String headerName = headerMatcher.group(1);
			return request -> {
				request.setCookies(result.getResponse().getCookie("XSRF-TOKEN"));
				request.addHeader(headerName, token);
				return request;
			};
		} catch (Exception exception) {
			throw new IllegalStateException("Could not request a CSRF token for the test.", exception);
		}
	}
}
