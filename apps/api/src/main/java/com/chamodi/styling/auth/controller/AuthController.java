package com.chamodi.styling.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DeferredCsrfToken;

import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.dto.RegisterResponse;
import com.chamodi.styling.auth.dto.AccountResponse;
import com.chamodi.styling.auth.dto.CsrfResponse;
import com.chamodi.styling.auth.dto.LoginRequest;
import com.chamodi.styling.auth.security.AccountPrincipal;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.service.AuthService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository securityContextRepository;
	private final LogoutHandler logoutHandler;

	public AuthController(AuthService authService, AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			SecurityContextRepository securityContextRepository, LogoutHandler logoutHandler) {
		this.authService = authService;
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.securityContextRepository = securityContextRepository;
		this.logoutHandler = logoutHandler;
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = authService.register(request);
		RegisterResponse response = new RegisterResponse(user.getId(), user.getFullName(), user.getEmail());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/csrf")
	public CsrfResponse csrf(HttpServletRequest request) {
		// The SPA header uses the repository token, not the XOR-masked CsrfToken request attribute.
		DeferredCsrfToken deferred = (DeferredCsrfToken) request.getAttribute(DeferredCsrfToken.class.getName());
		CsrfToken token = deferred.get();
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	@PostMapping("/login")
	public AccountResponse login(@Valid @RequestBody LoginRequest loginRequest,
			HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(loginRequest.email(), loginRequest.password()));
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		return account((AccountPrincipal) authentication.getPrincipal());
	}

	@GetMapping("/me")
	public AccountResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
		return account(principal);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		logoutHandler.logout(request, response, authentication);
	}

	private AccountResponse account(AccountPrincipal principal) {
		return new AccountResponse(principal.getId(), principal.getFullName(), principal.getEmail());
	}

}
