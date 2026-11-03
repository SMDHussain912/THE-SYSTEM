package com.thesystem.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;\nimport org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;\nimport org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.thesystem.backend.account.AccountRepository;

@Configuration
public class SecurityConfiguration {
	@Bean
	PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

	@Bean
	UserDetailsService userDetailsService(AccountRepository accounts) {
		return email -> accounts.findByEmail(AccountService.normalizeEmail(email))
				.map(account -> User.withUsername(account.getEmail())
						.password(account.getPasswordHash() == null ? "" : account.getPasswordHash())
						.roles("USER").build())
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
		provider.setPasswordEncoder(encoder);
		return new ProviderManager(provider);
	}

	@Bean
	CookieCsrfTokenRepository csrfTokenRepository(
			@Value("${app.security.cookie-secure:false}") boolean secureCookies) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax").secure(secureCookies));
		return repository;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CookieCsrfTokenRepository csrfTokenRepository,
			BearerTokenResolver jwtCookieBearerTokenResolver,
			JwtOAuth2SuccessHandler oauth2SuccessHandler,
			GoogleAccountOAuth2UserService googleAccountOAuth2UserService,
			@Value("${app.security.google-enabled:false}") boolean googleEnabled,
			@Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) throws Exception {
		http
			.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
					.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.securityContext(context -> context.requireExplicitSave(true))
			.authorizeHttpRequests(authorize -> authorize
					.requestMatchers("/api/health", "/api/auth/csrf", "/api/auth/register", "/api/auth/login",
							"/oauth2/**", "/login/oauth2/**").permitAll()
					.requestMatchers("/api/auth/**").authenticated()
					.anyRequest().denyAll())
			.exceptionHandling(exceptions -> exceptions
					.authenticationEntryPoint((request, response, exception) ->
							response.sendError(HttpStatus.UNAUTHORIZED.value()))
					.accessDeniedHandler((request, response, exception) ->
							response.sendError(HttpStatus.FORBIDDEN.value())))
			.oauth2ResourceServer(resourceServer -> resourceServer
					.bearerTokenResolver(jwtCookieBearerTokenResolver)
					.jwt(jwt -> {}))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable);
		if (googleEnabled) {
			http.oauth2Login(oauth -> oauth
					.userInfoEndpoint(userInfo -> userInfo.userService(new GoogleAccountOAuth2UserServiceProxy()))
					.successHandler(oauth2SuccessHandler)
					.failureUrl(frontendUrl + "/?authError=google"));
		}
		return http.build();
	}
}
