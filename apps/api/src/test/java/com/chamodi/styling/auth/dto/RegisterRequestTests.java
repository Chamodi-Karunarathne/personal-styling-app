package com.chamodi.styling.auth.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;

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
		assertViolation(new RegisterRequest("Example User", "user@example.com", "a".repeat(7)),
				"password", "Password must be at least 8 characters");
		assertViolation(new RegisterRequest("Example User", "user@example.com", "a".repeat(73)),
				"password", "Password must be at most 72 UTF-8 bytes");
	}

	@Test
	void rejectsWhitespacePasswordEvenWithinLengthLimits() {
		assertViolation(new RegisterRequest("Example User", "user@example.com", " ".repeat(8)),
				"password", "Password is required");
	}

	@Test
	void acceptsAccentedPasswordAtExactly72Bytes() {
		String password = "\u00e9".repeat(36);
		assertEquals(72, password.getBytes(StandardCharsets.UTF_8).length);
		assertTrue(validator.validate(new RegisterRequest("Example User", "user@example.com", password)).isEmpty());
	}

	@Test
	void acceptsEmojiPasswordAtExactly72Bytes() {
		String password = "a".repeat(68) + "\uD83D\uDE00";
		assertEquals(72, password.getBytes(StandardCharsets.UTF_8).length);
		assertTrue(validator.validate(new RegisterRequest("Example User", "user@example.com", password)).isEmpty());
	}

	@Test
	void rejectsEmojiPasswordAbove72BytesEvenWhenCharacterLengthFits() {
		String password = "a".repeat(69) + "\uD83D\uDE00";
		assertEquals(71, password.length());
		assertEquals(73, password.getBytes(StandardCharsets.UTF_8).length);
		assertViolation(new RegisterRequest("Example User", "user@example.com", password),
				"password", "Password must be at most 72 UTF-8 bytes");
	}

	@Test
	void rejectsAccentedPasswordAbove72Bytes() {
		String password = "\u00e9".repeat(37);
		assertEquals(74, password.getBytes(StandardCharsets.UTF_8).length);
		assertViolation(new RegisterRequest("Example User", "user@example.com", password),
				"password", "Password must be at most 72 UTF-8 bytes");
	}

	@Test
	void validationPreservesUnicodeAndSurroundingPasswordWhitespace() {
		String password = "  e\u0301\u00e9Password123  ";
		RegisterRequest request = new RegisterRequest("Example User", "user@example.com", password);

		assertTrue(validator.validate(request).isEmpty());
		assertEquals(password, request.password());
	}

	@Test
	void stringRepresentationRedactsPasswordWithoutChangingRequestContents() {
		String password = "SensitiveRegistrationProbe123";
		RegisterRequest request = new RegisterRequest("Example User", "user@example.com", password);

		assertFalse(request.toString().contains(password));
		assertTrue(request.toString().contains("[REDACTED]"));
		assertEquals(password, request.password());
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
