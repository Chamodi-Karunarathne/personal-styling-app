package com.chamodi.styling.auth.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RegisterRequestTests {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void acceptsValidRequestWithoutPasswordComplexityRules() {
		assertTrue(validator.validate(new RegisterRequest(
				"Example User", "user@example.com", "abcdefgh")).isEmpty());
	}

	@Test
	void acceptsMaximumLengths() {
		assertTrue(validator.validate(new RegisterRequest(
				"a".repeat(150), emailWithLength(254), "a".repeat(72))).isEmpty());
	}

	@Test
	void rejectsNullEmptyAndWhitespaceOnlyRequiredFields() {
		for (String value : new String[] { null, "", " \t\n" }) {
			assertViolation(new RegisterRequest(value, "user@example.com", "abcdefgh"),
					"fullName", "Full name is required");
			assertViolation(new RegisterRequest("Example User", value, "abcdefgh"),
					"email", "Email is required");
			assertViolation(new RegisterRequest("Example User", "user@example.com", value),
					"password", "Password is required");
		}
	}

	@Test
	void rejectsInvalidEmailFormat() {
		assertViolation(new RegisterRequest("Example User", "not-an-email", "abcdefgh"),
				"email", "Email must be valid");
	}

	@Test
	void rejectsFullNameAboveMaximumLength() {
		assertViolation(new RegisterRequest("a".repeat(151), "user@example.com", "abcdefgh"),
				"fullName", "Full name must be at most 150 characters");
	}

	@Test
	void rejectsEmailAboveMaximumLength() {
		assertViolation(new RegisterRequest("Example User", emailWithLength(255), "abcdefgh"),
				"email", "Email must be at most 254 characters");
	}

	@Test
	void rejectsPasswordsOutsideLengthLimits() {
		for (int length : new int[] { 7, 73 }) {
			assertViolation(new RegisterRequest("Example User", "user@example.com", "a".repeat(length)),
					"password", "Password must be between 8 and 72 characters");
		}
	}

	@Test
	void rejectsWhitespacePasswordEvenWithinLengthLimits() {
		assertViolation(new RegisterRequest("Example User", "user@example.com", " ".repeat(8)),
				"password", "Password is required");
	}

	private static String emailWithLength(int length) {
		String prefix = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + ".";
		return prefix + "d".repeat(length - prefix.length());
	}

	private static void assertViolation(RegisterRequest request, String field, String message) {
		assertTrue(validator.validate(request).stream().anyMatch(violation ->
				violation.getPropertyPath().toString().equals(field)
						&& violation.getMessage().equals(message)),
				"Expected validation message for " + field + ": " + message);
	}

}
