package com.chamodi.styling.auth.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, String> {

	private int maxBytes;

	@Override
	public void initialize(Utf8ByteLength constraint) {
		maxBytes = constraint.max();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		// Required values are checked separately by @NotBlank. Never alter the password.
		return value == null || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
	}

}
