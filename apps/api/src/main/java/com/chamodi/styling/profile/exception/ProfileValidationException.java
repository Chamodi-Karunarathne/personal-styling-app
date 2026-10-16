package com.chamodi.styling.profile.exception;

import java.util.Map;

public class ProfileValidationException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public ProfileValidationException(Map<String, String> fieldErrors) {
		super("Validation failed");
		this.fieldErrors = Map.copyOf(fieldErrors);
	}

	public Map<String, String> getFieldErrors() {
		return fieldErrors;
	}
}
