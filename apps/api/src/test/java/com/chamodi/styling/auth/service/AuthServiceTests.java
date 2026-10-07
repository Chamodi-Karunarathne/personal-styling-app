package com.chamodi.styling.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.exception.EmailAlreadyExistsException;
import com.chamodi.styling.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder);
	}

	@Test
	void registersWithNormalizedDetailsAndOnlyTheEncodedPassword() {
		String rawPassword = "  ExamplePassword123  ";
		String encodedPassword = "encoded-password-hash";
		RegisterRequest request = new RegisterRequest(
				"  Example User  ", "  Person@Example.COM  ", rawPassword);
		User savedUser = new User("Example User", "person@example.com", encodedPassword);

		when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
		when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		User result = authService.register(request);

		verify(userRepository).existsByEmail("person@example.com");
		verify(passwordEncoder).encode(rawPassword);
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User userToSave = userCaptor.getValue();
		assertEquals("Example User", userToSave.getFullName());
		assertEquals("person@example.com", userToSave.getEmail());
		assertEquals(encodedPassword, userToSave.getPasswordHash());
		assertNotEquals(rawPassword, userToSave.getPasswordHash());
		assertSame(savedUser, result);
	}

	@Test
	void rejectsDuplicateNormalizedEmailWithoutEncodingOrSaving() {
		RegisterRequest request = new RegisterRequest(
				"  Example User  ", "  Person@Example.COM  ", "ExamplePassword123");
		when(userRepository.existsByEmail("person@example.com")).thenReturn(true);

		EmailAlreadyExistsException exception = assertThrows(
				EmailAlreadyExistsException.class, () -> authService.register(request));

		assertEquals("Email is already registered", exception.getMessage());
		verify(userRepository).existsByEmail("person@example.com");
		verifyNoInteractions(passwordEncoder);
		verify(userRepository, never()).save(any(User.class));
	}

}
