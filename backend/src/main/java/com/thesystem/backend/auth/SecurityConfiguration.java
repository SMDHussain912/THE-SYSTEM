package com.thesystem.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.thesystem.backend.account.AccountRepository;

@Configuration
public class SecurityConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	UserDetailsService userDetailsService(AccountRepository accounts) {
		return email -> accounts.findByEmail(AccountService.normalizeEmail(email))
				.map(account -> User.withUsername(account.getEmail())
						.password(account.getPasswordHash() == null ? "" : account.getPasswordHash())
						.roles("USER")
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
	}

	@Bean
	AuthenticationManager authenticationManager(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CookieCsrfTokenRepository csrfTokenRepository(
			@Value("${app.security.cookie-secure:false}") boolean secureCookies) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie
				.path("/")
				.sameSite("Lax")
				.secure(secureCookies));
		return repository;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CookieCsrfTokenRepository csrfTokenRepository,
			SecurityContextRepository securityContextRepository,
			GoogleAccountOAuth2UserService googleAccountOAuth2UserService) throws Exception {
		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.securityContext(securityContext -> securityContext
						.securityContextRepository(securityContextRepository))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/health", "/api/auth/csrf", "/api/auth/register", "/api/auth/login", "/oauth2/**", "/login/oauth2/**")
						.permitAll()
						.requestMatchers("/api/auth/**").authenticated()
						.anyRequest().denyAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								response.sendError(HttpStatus.UNAUTHORIZED.value()))
						.accessDeniedHandler((request, response, exception) ->
								response.sendError(HttpStatus.FORBIDDEN.value())))
				.oauth2Login(oauth -> oauth
						.userInfoEndpoint(userInfo -> userInfo.userService(googleAccountOAuth2UserService))
						.defaultSuccessUrl("/", true))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable);
		return http.build();
	}
}
