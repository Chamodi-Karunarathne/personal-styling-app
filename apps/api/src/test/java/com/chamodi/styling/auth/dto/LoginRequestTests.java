package com.chamodi.styling.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

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

	@Test
	void acceptsAccentedPasswordAtExactly72Bytes() {
		String password = "\u00e9".repeat(36);
		assertEquals(72, password.getBytes(StandardCharsets.UTF_8).length);
		assertTrue(validator.validate(new LoginRequest("user@example.com", password)).isEmpty());
	}

	@Test
	void acceptsEmojiPasswordAtExactly72Bytes() {
		String password = "a".repeat(68) + "\uD83D\uDE00";
		assertEquals(72, password.getBytes(StandardCharsets.UTF_8).length);
		assertTrue(validator.validate(new LoginRequest("user@example.com", password)).isEmpty());
	}

	@Test
	void rejectsEmojiPasswordAbove72BytesEvenWhenCharacterLengthFits() {
		String password = "a".repeat(69) + "\uD83D\uDE00";
		assertEquals(71, password.length());
		assertEquals(73, password.getBytes(StandardCharsets.UTF_8).length);
		var violations = validator.validate(new LoginRequest("user@example.com", password));
		assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
				&& violation.getMessage().equals("Password must be at most 72 UTF-8 bytes")));
	}

	@Test
	void rejectsAccentedPasswordAbove72Bytes() {
		String password = "\u00e9".repeat(37);
		assertEquals(74, password.getBytes(StandardCharsets.UTF_8).length);
		assertViolation(new LoginRequest("user@example.com", password), "password");
	}

	@Test
	void validationPreservesUnicodeAndSurroundingPasswordWhitespace() {
		String password = "  e\u0301\u00e9Password123  ";
		LoginRequest request = new LoginRequest("user@example.com", password);

		assertTrue(validator.validate(request).isEmpty());
		assertEquals(password, request.password());
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
