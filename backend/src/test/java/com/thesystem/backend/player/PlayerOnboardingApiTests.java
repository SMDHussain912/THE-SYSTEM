package com.thesystem.backend.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.backend.account.Account;
import com.thesystem.backend.account.AccountRepository;
import com.thesystem.backend.auth.AccountService;
import com.thesystem.backend.auth.AuthRequests;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlayerOnboardingApiTests {
	private static final String PASSWORD = "LongEnoughPassword123!";

	@Autowired private MockMvc mockMvc;
	@Autowired private AccountRepository accountRepository;
	@Autowired private AccountService accountService;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private PasswordEncoder passwordEncoder;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void clearAccounts() {
		accountRepository.deleteAll();
	}

	@Test
	void onboardingRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/player/onboarding"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void profileUpdateRequiresCsrf() throws Exception {
		UserSession user = createUser("csrf-player@example.com");
		mockMvc.perform(put("/api/player/profile").with(authOnly(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(profileJson(0, List.of())))
				.andExpect(status().isForbidden());
	}

	@Test
	void profileIsPrivateToItsOwnerAndReturnsOnlyPlayerData() throws Exception {
		UserSession first = createUser("first-player@example.com");
		UserSession second = createUser("second-player@example.com");

		mockMvc.perform(put("/api/player/profile").with(authenticated(first.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(profileJson(0, List.of("Fear of failure", "Self-doubt"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.playerName").value("First Player"))
				.andExpect(jsonPath("$.mainChallenge").value("Procrastination"))
				.andExpect(jsonPath("$.additionalChallenges[0]").value("Fear of failure"))
				.andExpect(jsonPath("$.additionalChallenges[1]").value("Self-doubt"))
				.andExpect(jsonPath("$.goal").value("Build consistency"))
				.andExpect(jsonPath("$.situation").value("Balancing work and study"))
				.andExpect(jsonPath("$.status").value("READY"))
				.andExpect(jsonPath("$.currentChapter").value("INITIALIZATION"))
				.andExpect(jsonPath("$.email").doesNotExist());

		mockMvc.perform(get("/api/player/onboarding").with(authenticated(first.accessCookie())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.playerName").value("First Player"));

		mockMvc.perform(get("/api/player/onboarding").with(authenticated(second.accessCookie())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.playerName").value("second player"))
				.andExpect(jsonPath("$.mainChallenge").doesNotExist())
				.andExpect(jsonPath("$.additionalChallenges").isEmpty());
	}

	@Test
	void profileRejectsExcessiveAdditionalChallenges() throws Exception {
		UserSession user = createUser("challenge-limit@example.com");
		mockMvc.perform(put("/api/player/profile").with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(profileJson(0, List.of("One", "Two", "Three", "Four", "Five", "Six"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void profileRejectsDuplicateChallenges() throws Exception {
		UserSession user = createUser("duplicate-challenge@example.com");
		mockMvc.perform(put("/api/player/profile").with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(profileJson(0, List.of("Fear of failure", "  fear of failure  "))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void progressRequiresProfileAndValidChapterChoice() throws Exception {
		UserSession user = createUser("progress-player@example.com");
		mockMvc.perform(post("/api/player/awakening/choices").with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(choiceJson("INITIALIZATION", "CONTINUE", 0)))
				.andExpect(status().isConflict());

		long version = updateProfile(user, List.of()).get("version").asLong();
		mockMvc.perform(post("/api/player/awakening/choices").with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(choiceJson("ENCOUNTER", "CONFRONT", version)))
				.andExpect(status().isConflict());
	}

	@Test
	void choicesOfferDeclineAcceptCompleteReplayAndSkipFollowValidTransitions() throws Exception {
		UserSession user = createUser("awakening-player@example.com");
		long version = updateProfile(user, List.of("Fear of failure", "Inconsistency")).get("version").asLong();

		version = recordChoice(user, "INITIALIZATION", "CONTINUE", version)
				.get("version").asLong();
		version = recordChoice(user, "IDENTIFICATION", "CONTINUE", version)
				.get("version").asLong();
		version = recordChoice(user, "ENCOUNTER", "OBSERVE", version)
				.get("version").asLong();
		JsonNode offer = recordChoice(user, "DESPAIR", "CONTINUE", version);
		assertEquals("SYSTEM_OFFER", offer.get("currentChapter").asText());
		assertEquals(4, offer.get("choices").size());
		version = offer.get("version").asLong();

		JsonNode declined = progressAction(user, "decline", version);
		assertNotNull(declined.get("declinedAt"));
		assertEquals("SYSTEM_OFFER", declined.get("currentChapter").asText());
		version = declined.get("version").asLong();

		JsonNode accepted = progressAction(user, "accept", version);
		assertNotNull(accepted.get("acceptedAt"));
		assertTrue(accepted.get("declinedAt").isNull());
		assertEquals("AWAKENING", accepted.get("currentChapter").asText());
		JsonNode acceptedRetry = progressAction(user, "accept", version);
		assertEquals(accepted.get("version").asLong(), acceptedRetry.get("version").asLong());
		version = accepted.get("version").asLong();

		JsonNode completed = progressAction(user, "complete", version);
		assertEquals("COMPLETED", completed.get("status").asText());
		JsonNode completedRetry = progressAction(user, "complete", version);
		assertEquals(completed.get("version").asLong(), completedRetry.get("version").asLong());
		version = completed.get("version").asLong();

		JsonNode replay = progressAction(user, "replay", version);
		assertEquals("READY", replay.get("status").asText());
		assertEquals("INITIALIZATION", replay.get("currentChapter").asText());
		assertTrue(replay.get("choices").isEmpty());
		assertTrue(replay.get("acceptedAt").isNull());
		version = replay.get("version").asLong();

		JsonNode skipped = progressAction(user, "skip", version);
		assertEquals("SKIPPED", skipped.get("status").asText());
		assertNotNull(skipped.get("skippedAt"));
		assertTrue(skipped.get("completedAt").isTextual());
	}

	@Test
	void replayAllowsLegacyCompletedAccountWithoutPlayerProfile() throws Exception {
		UserSession user = createUser("legacy-player@example.com");
		Account account = accountRepository.findByEmail("legacy-player@example.com").orElseThrow();
		jdbcTemplate.update(
				"UPDATE player_onboarding SET status = 'COMPLETED', current_chapter = 'AWAKENING', completed_at = CURRENT_TIMESTAMP WHERE account_id = ?",
				account.getId());

		JsonNode replay = progressAction(user, "replay", 0);

		assertEquals("PROFILE_REQUIRED", replay.get("status").asText());
		assertEquals("INITIALIZATION", replay.get("currentChapter").asText());
		assertTrue(replay.get("mainChallenge").isNull());
		assertTrue(replay.get("goal").isNull());
	}

	@Test
	void staleVersionCannotOverwriteProgress() throws Exception {
		UserSession user = createUser("stale-version@example.com");
		updateProfile(user, List.of());

		mockMvc.perform(put("/api/player/profile").with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(profileJson(0, List.of())))
				.andExpect(status().isConflict());
	}

	private JsonNode updateProfile(UserSession user, List<String> challenges) throws Exception {
		MvcResult result = mockMvc.perform(put("/api/player/profile").with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(profileJson(0, challenges)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private JsonNode recordChoice(UserSession user, String chapter, String choice, long version) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/player/awakening/choices")
						.with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(choiceJson(chapter, choice, version)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private JsonNode progressAction(UserSession user, String action, long version) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/player/awakening/" + action)
						.with(authenticated(user.accessCookie()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("version", version))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	private String profileJson(long version, List<String> challenges) throws Exception {
		return objectMapper.writeValueAsString(Map.of(
				"playerName", "First Player",
				"mainChallenge", "Procrastination",
				"additionalChallenges", challenges,
				"goal", "Build consistency",
				"situation", "Balancing work and study",
				"version", version));
	}

	private String choiceJson(String chapter, String choice, long version) throws Exception {
		return objectMapper.writeValueAsString(Map.of(
				"chapter", chapter,
				"choice", choice,
				"version", version));
	}

	private UserSession createUser(String email) throws Exception {
		String name = email.substring(0, email.indexOf('@')).replace('-', ' ');
		Account account = accountService.register(new AuthRequests.Register(name, email, PASSWORD));
		account.setEmailVerified(true);
		accountRepository.save(account);

		MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrfOnly())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
				.andExpect(status().isOk())
				.andReturn();
		return new UserSession(result.getResponse().getCookie("SYSTEM_ACCESS"));
	}

	private RequestPostProcessor authenticated(Cookie accessCookie) {
		try {
			MockHttpServletResponse csrfResponse = mockMvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
			JsonNode token = objectMapper.readTree(csrfResponse.getContentAsString());
			String headerName = token.get("headerName").asText();
			String csrfToken = token.get("token").asText();
			Cookie csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");
			return request -> {
				request.setCookies(accessCookie, csrfCookie);
				request.addHeader(headerName, csrfToken);
				return request;
			};
		} catch (Exception exception) {
			throw new IllegalStateException("Could not request a CSRF token for the test.", exception);
		}
	}

	private RequestPostProcessor authOnly(Cookie accessCookie) {
		return request -> {
			request.setCookies(accessCookie);
			return request;
		};
	}

	private RequestPostProcessor csrfOnly() {
		try {
			MockHttpServletResponse csrfResponse = mockMvc.perform(get("/api/auth/csrf")).andReturn().getResponse();
			JsonNode token = objectMapper.readTree(csrfResponse.getContentAsString());
			String headerName = token.get("headerName").asText();
			String csrfToken = token.get("token").asText();
			Cookie csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");
			return request -> {
				request.setCookies(csrfCookie);
				request.addHeader(headerName, csrfToken);
				return request;
			};
		} catch (Exception exception) {
			throw new IllegalStateException("Could not request a CSRF token for the test.", exception);
		}
	}

	private record UserSession(Cookie accessCookie) {}
}
