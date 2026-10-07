package com.chamodi.styling.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.dto.RegisterResponse;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = authService.register(request);
		RegisterResponse response = new RegisterResponse(user.getId(), user.getFullName(), user.getEmail());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

}
