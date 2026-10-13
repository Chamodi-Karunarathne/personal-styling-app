package com.chamodi.styling.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class LoginRequestTests {

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
	void acceptsValidLogin() {
		assertTrue(validator.validate(new LoginRequest("user@example.com", "ExamplePassword123")).isEmpty());
	}

	@Test
	void trimsSurroundingEmailWhitespaceWithoutChangingTheRawPassword() {
		String password = "  ExamplePassword123  ";
		LoginRequest request = new LoginRequest("  Person@Example.COM  ", password);

		assertEquals("Person@Example.COM", request.email());
		assertEquals(password, request.password());
		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void allowsShortNonBlankPasswordsToReachCredentialVerification() {
		assertTrue(validator.validate(new LoginRequest("user@example.com", "a")).isEmpty());
	}

	@Test
	void acceptsMaximumLengths() {
		assertTrue(validator.validate(new LoginRequest(emailWithLength(254), "a".repeat(72))).isEmpty());
	}

	@Test
	void rejectsNullEmptyAndWhitespaceOnlyRequiredFields() {
		for (String value : new String[] { null, "", " \t\n" }) {
			assertViolation(new LoginRequest(value, "ExamplePassword123"), "email");
			assertViolation(new LoginRequest("user@example.com", value), "password");
		}
	}

	@Test
	void rejectsInvalidEmailFormat() {
		assertViolation(new LoginRequest("not-an-email", "ExamplePassword123"), "email");
	}

	@Test
	void rejectsEmailAboveMaximumLength() {
		assertViolation(new LoginRequest(emailWithLength(255), "ExamplePassword123"), "email");
	}

	@Test
	void rejectsPasswordAboveMaximumLength() {
		assertViolation(new LoginRequest("user@example.com", "a".repeat(73)), "password");
	}

	private static String emailWithLength(int length) {
		String prefix = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + ".";
		return prefix + "d".repeat(length - prefix.length());
	}

	private static void assertViolation(LoginRequest request, String field) {
		assertTrue(validator.validate(request).stream().anyMatch(violation ->
				violation.getPropertyPath().toString().equals(field)),
				"Expected a validation error for " + field);
	}

}
