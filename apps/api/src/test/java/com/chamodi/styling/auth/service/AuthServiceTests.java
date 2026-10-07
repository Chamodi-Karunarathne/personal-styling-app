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

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.internal.StandardTableExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
		when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

		User result = authService.register(request);

		verify(userRepository).existsByEmail("person@example.com");
		verify(passwordEncoder).encode(rawPassword);
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
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
		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

	@Test
	void rejectsEmailUniquenessRaceUsingTheExistingHibernateConstraint() {
		// Obtain the actual constraint name from the current mapping, without JDBC or schema execution.
		String constraintName = mappedEmailConstraintName().toLowerCase(Locale.ROOT);
		DataIntegrityViolationException failure = constraintFailure("23505", constraintName);
		RegisterRequest request = prepareSaveFailure(failure);

		EmailAlreadyExistsException exception = assertThrows(
				EmailAlreadyExistsException.class, () -> authService.register(request));

		assertEquals("Email is already registered", exception.getMessage());
		verify(userRepository).existsByEmail("person@example.com");
		verify(userRepository).saveAndFlush(any(User.class));
	}

	@Test
	void doesNotMisclassifyOtherUniqueConstraintsAsDuplicateEmail() {
		DataIntegrityViolationException failure = constraintFailure("23505", "app_users_pkey");
		RegisterRequest request = prepareSaveFailure(failure);

		assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> authService.register(request)));
	}

	@Test
	void doesNotMisclassifyNonUniqueViolationsAsDuplicateEmail() {
		DataIntegrityViolationException failure = constraintFailure("23502", mappedEmailConstraintName());
		RegisterRequest request = prepareSaveFailure(failure);

		assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> authService.register(request)));
	}

	@Test
	void preservesUnclassifiedIntegrityFailures() {
		DataIntegrityViolationException failure = new DataIntegrityViolationException("Persistence failed");
		RegisterRequest request = prepareSaveFailure(failure);

		assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> authService.register(request)));
	}

	private RegisterRequest prepareSaveFailure(DataIntegrityViolationException failure) {
		RegisterRequest request = new RegisterRequest("Example User", "Person@Example.COM", "ExamplePassword123");
		when(passwordEncoder.encode(request.password())).thenReturn("encoded-password-hash");
		when(userRepository.saveAndFlush(any(User.class))).thenThrow(failure);
		return request;
	}

	private DataIntegrityViolationException constraintFailure(String sqlState, String constraintName) {
		SQLException sqlException = new SQLException("Database constraint rejected the insert", sqlState);
		ConstraintViolationException violation = new ConstraintViolationException(
				"Constraint rejected the insert", sqlException, constraintName);
		return new DataIntegrityViolationException("Persistence failed", new IllegalStateException(violation));
	}

	private String mappedEmailConstraintName() {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
				.applySetting("hibernate.dialect", PostgreSQLDialect.class.getName())
				.build();
		try {
			Metadata metadata = new MetadataSources(registry).addAnnotatedClass(User.class).buildMetadata();
			Table table = metadata.getEntityBinding(User.class.getName()).getTable();
			var database = metadata.getDatabase();
			var context = SqlStringGenerationContextImpl.fromConfigurationMap(
					database.getJdbcEnvironment(), database, Map.of());
			new StandardTableExporter(database.getDialect()).getSqlCreateStrings(table, metadata, context);
			return table.getColumn(new Column("email")).getUniqueKeyName();
		} finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

}
