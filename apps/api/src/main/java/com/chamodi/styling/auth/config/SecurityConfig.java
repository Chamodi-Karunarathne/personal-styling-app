package com.chamodi.styling.auth.config;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.chamodi.styling.auth.security.ApiSecurityErrorHandler;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(
				new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
	}

	@Bean
	public CookieCsrfTokenRepository csrfTokenRepository(@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax").secure(secure));
		return repository;
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy(CookieCsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(
				new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}

	@Bean
	public LogoutHandler logoutHandler(SecurityContextRepository securityContextRepository,
			CookieCsrfTokenRepository csrfTokenRepository,
			@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
		SecurityContextLogoutHandler contextHandler = new SecurityContextLogoutHandler();
		contextHandler.setSecurityContextRepository(securityContextRepository);
		Cookie sessionCookie = new Cookie("JSESSIONID", "");
		sessionCookie.setPath("/");
		sessionCookie.setMaxAge(0);
		sessionCookie.setHttpOnly(true);
		sessionCookie.setSecure(secure);
		sessionCookie.setAttribute("SameSite", "Lax");
		return new CompositeLogoutHandler(new CsrfLogoutHandler(csrfTokenRepository), contextHandler,
				new CookieClearingLogoutHandler(sessionCookie));
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(@Value("${app.security.frontend-origin}") String frontendOrigin) {
		URI origin = URI.create(frontendOrigin);
		if (!("http".equals(origin.getScheme()) || "https".equals(origin.getScheme())) || origin.getHost() == null
				|| origin.getRawUserInfo() != null || !origin.getRawPath().isEmpty()
				|| origin.getRawQuery() != null || origin.getRawFragment() != null) {
			throw new IllegalArgumentException("Frontend origin must be one exact HTTP or HTTPS origin");
		}
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(frontendOrigin));
		configuration.setAllowCredentials(true);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfTokenRepository,
			CorsConfigurationSource corsConfigurationSource, ApiSecurityErrorHandler errorHandler) throws Exception {
		http
				.authenticationManager(authenticationManager)
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
				.requestCache(cache -> cache.requestCache(new NullRequestCache()))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.exceptionHandling(errors -> errors.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
				.authorizeHttpRequests(authorize -> authorize
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						// MVC returns 405 for unsupported methods on these public endpoints.
						.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll());
		return http.build();
	}

}
