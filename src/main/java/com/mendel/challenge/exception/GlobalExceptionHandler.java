package com.mendel.challenge.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates exceptions into RFC 9457 "problem+json" responses.
 *
 * Spring picks the handler whose exception type is the closest match:
 * - Spring's built-in errors (validation failures, malformed JSON, 405, ...) are handled
 *   by the parent class.
 * - Our own exceptions are handled by the specific methods below.
 * - Anything else falls through to {@link #handleUnexpected}, the catch-all.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/**
	 * Catch-all for unexpected errors (bugs, database failures, ...).
	 *
	 * The full details (exception, message, stack trace, request) go to the server log only.
	 * The client gets a generic message plus an errorId that matches the log entry, so a
	 * reported failure can be traced without exposing internals.
	 */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		String errorId = UUID.randomUUID().toString();

		// Passing `ex` as the last argument makes SLF4J log the full stack trace
		log.error("Unhandled exception [errorId={}] {} {}",
				errorId, request.getMethod(), request.getRequestURI(), ex);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
		problem.setProperty("errorId", errorId);
		return problem;
	}
}
