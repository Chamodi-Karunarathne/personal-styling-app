package com.chamodi.styling.auth.dto;

import com.chamodi.styling.auth.validation.Utf8ByteLength;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 150, message = "Full name must be at most 150 characters")
		String fullName,

		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 254, message = "Email must be at most 254 characters")
		String email,

		@NotBlank(message = "Password is required")
		@Size(min = 8, message = "Password must be at least 8 characters")
		@Utf8ByteLength(max = 72, message = "Password must be at most {max} UTF-8 bytes")
		String password
) {

	@Override
	public String toString() {
		return "RegisterRequest[credentials=[REDACTED]]";
	}

}
