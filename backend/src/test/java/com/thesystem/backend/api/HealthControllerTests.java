package com.thesystem.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thesystem.backend.account.AccountRepository;
import com.thesystem.backend.auth.GoogleAccountOAuth2UserService;
import com.thesystem.backend.auth.JwtConfiguration;
import com.thesystem.backend.auth.JwtCookieBearerTokenResolver;
import com.thesystem.backend.auth.JwtOAuth2SuccessHandler;
import com.thesystem.backend.auth.SecurityConfiguration;

@WebMvcTest(HealthController.class)
@Import({ SecurityConfiguration.class, JwtConfiguration.class, JwtCookieBearerTokenResolver.class })
@ActiveProfiles("test")
class HealthControllerTests {

	@MockitoBean
	private AccountRepository accountRepository;

	@MockitoBean
	private JwtOAuth2SuccessHandler oauth2SuccessHandler;

	@MockitoBean
	private GoogleAccountOAuth2UserService googleAccountOAuth2UserService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointReturnsOk() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}
}
