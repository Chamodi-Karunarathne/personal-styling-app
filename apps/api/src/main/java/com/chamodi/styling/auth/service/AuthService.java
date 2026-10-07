package com.chamodi.styling.auth.service;

import java.util.Locale;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chamodi.styling.auth.dto.RegisterRequest;
import com.chamodi.styling.auth.entity.User;
import com.chamodi.styling.auth.exception.EmailAlreadyExistsException;
import com.chamodi.styling.auth.repository.UserRepository;

@Service
public class AuthService {

	// Match the existing @Column(unique = true) constraint without altering the schema.
	// Revisit this if the table, column, or Hibernate implicit naming strategy changes.
	private static final String EMAIL_UNIQUE_CONSTRAINT = NamingHelper.INSTANCE.generateHashedConstraintName(
			"UK", Identifier.toIdentifier("app_users"), Identifier.toIdentifier("email"));

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(RegisterRequest request) {
		String fullName = request.fullName().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);

		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException();
		}

		String passwordHash = passwordEncoder.encode(request.password());
		User user = new User(fullName, email, passwordHash);

		try {
			// Flush here so a uniqueness race is handled before the transaction returns.
			return userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			if (isEmailUniqueViolation(exception)) {
				throw new EmailAlreadyExistsException();
			}
			throw exception;
		}
	}

	private boolean isEmailUniqueViolation(DataIntegrityViolationException exception) {
		for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return "23505".equals(violation.getSQLState())
						&& EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName());
			}
		}
		return false;
	}

}
