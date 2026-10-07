package com.chamodi.styling.common.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.chamodi.styling.auth.exception.EmailAlreadyExistsException;
import com.chamodi.styling.common.dto.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<Object> handleDuplicateEmail(EmailAlreadyExistsException exception, WebRequest request) {
		return error(HttpStatus.CONFLICT, "Email is already registered", Map.of(), request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		// Prefer required-field messages when multiple constraints fail on an empty value.
		exception.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparingInt((FieldError field) -> "NotBlank".equals(field.getCode()) ? 0 : 1)
						.thenComparing(FieldError::getField)
						.thenComparing(field -> field.getDefaultMessage() == null ? "" : field.getDefaultMessage()))
				.forEach(field -> fieldErrors.putIfAbsent(field.getField(),
						field.getDefaultMessage() == null ? "Invalid value" : field.getDefaultMessage()));
		return error(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request body must be valid JSON", Map.of(), request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// Retain standard MVC status codes/headers without exposing framework exception details.
		HttpStatus httpStatus = HttpStatus.valueOf(status.value());
		return new ResponseEntity<>(body(httpStatus, httpStatus.getReasonPhrase(), Map.of(), request), headers, status);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of(), request);
	}

	private ResponseEntity<Object> error(HttpStatus status, String message,
			Map<String, String> fieldErrors, WebRequest request) {
		return ResponseEntity.status(status).body(body(status, message, fieldErrors, request));
	}

	private ApiErrorResponse body(HttpStatus status, String message,
			Map<String, String> fieldErrors, WebRequest request) {
		String path = ((ServletWebRequest) request).getRequest().getRequestURI();
		return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
	}

}
