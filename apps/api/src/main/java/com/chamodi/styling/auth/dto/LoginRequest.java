package com.chamodi.styling.auth.dto;

import com.chamodi.styling.auth.validation.Utf8ByteLength;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 254, message = "Email must be at most 254 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Utf8ByteLength(max = 72, message = "Password must be at most {max} UTF-8 bytes")
		String password) {

	public LoginRequest {
		email = email == null ? null : email.trim();
	}

	@Override
	public String toString() {
		return "LoginRequest[credentials=[REDACTED]]";
	}

}
