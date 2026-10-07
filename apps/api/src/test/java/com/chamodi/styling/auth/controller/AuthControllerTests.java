package com.chamodi.styling.auth.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.exception.EmailAlreadyExistsException;
import com.chamodi.styling.auth.service.AuthService;
import com.chamodi.styling.common.exception.GlobalExceptionHandler;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTests {

	private static final String REGISTER_PATH = "/api/auth/register";
	private static final String VALID_REQUEST = """
			{"fullName":"Example User","email":"user@example.com","password":"ExamplePassword123"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void createsAccountAndReturnsOnlySafeFields() throws Exception {
		User savedUser = mock(User.class);
		when(savedUser.getId()).thenReturn(1L);
		when(savedUser.getFullName()).thenReturn("Example User");
		when(savedUser.getEmail()).thenReturn("user@example.com");
		when(authService.register(any(RegisterRequest.class))).thenReturn(savedUser);

		mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", aMapWithSize(3)))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fullName").value("Example User"))
				.andExpect(jsonPath("$.email").value("user@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		verify(authService).register(new RegisterRequest("Example User", "user@example.com", "ExamplePassword123"));
	}

	@Test
	void rejectsInvalidEmailBeforeCallingService() throws Exception {
		assertValidationError(VALID_REQUEST.replace("user@example.com", "invalid-email"), "email", "Email must be valid");
	}

	@Test
	void rejectsBlankFullNameBeforeCallingService() throws Exception {
		assertValidationError(VALID_REQUEST.replace("Example User", "   "), "fullName", "Full name is required");
	}

	@Test
	void rejectsShortPasswordBeforeCallingService() throws Exception {
		assertValidationError(VALID_REQUEST.replace("ExamplePassword123", "short"),
				"password", "Password must be between 8 and 72 characters");
	}

	@Test
	void prefersRequiredMessageForEmptyPassword() throws Exception {
		assertValidationError(VALID_REQUEST.replace("ExamplePassword123", ""), "password", "Password is required");
	}

	@Test
	void returnsSafeConflictForDuplicateEmail() throws Exception {
		when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException());

		mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.error").value("Conflict"))
				.andExpect(jsonPath("$.message").value("Email is already registered"))
				.andExpect(jsonPath("$.path").value(REGISTER_PATH))
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(0)))
				.andExpect(jsonPath("$.stackTrace").doesNotExist())
				.andExpect(jsonPath("$.exception").doesNotExist());
	}

	@Test
	void rejectsMalformedJsonWithoutExposingParserDetails() throws Exception {
		mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content("{invalid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Request body must be valid JSON"));
		verifyNoInteractions(authService);
	}

	@Test
	void returnsSafeServerErrorForUnrelatedDatabaseFailure() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenThrow(new DataIntegrityViolationException("Internal SQL and constraint details"));

		mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"))
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(0)));
	}

	@Test
	void preservesMethodNotAllowedStatusWithConsistentErrorBody() throws Exception {
		mockMvc.perform(get(REGISTER_PATH))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message").value("Method Not Allowed"));
		verifyNoInteractions(authService);
	}

	@Test
	void doesNotLogRawPasswordFromValidRequest(CapturedOutput output) throws Exception {
		String password = "SensitiveLoggingProbe123";
		when(authService.register(any(RegisterRequest.class))).thenThrow(new EmailAlreadyExistsException());

		mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(VALID_REQUEST.replace("ExamplePassword123", password)))
				.andExpect(status().isConflict());

		assertFalse(output.getAll().contains(password));
	}

	@Test
	void doesNotLogRejectedPasswordFromValidationFailure(CapturedOutput output) throws Exception {
		String password = "pW!9";
		assertValidationError(VALID_REQUEST.replace("ExamplePassword123", password),
				"password", "Password must be between 8 and 72 characters");

		assertFalse(output.getAll().contains(password));
	}

	private void assertValidationError(String request, String field, String message) throws Exception {
		mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$", aMapWithSize(6)))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.path").value(REGISTER_PATH))
				.andExpect(jsonPath("$.fieldErrors." + field).value(message));
		verifyNoInteractions(authService);
	}

}
