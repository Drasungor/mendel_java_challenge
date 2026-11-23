package com.mendel.challenge.exception;

/**
 * Thrown by the service layer when a request is well-formed but breaks a business rule
 * (e.g. a transaction being its own parent). Mapped to HTTP 400.
 */
public class InvalidTransactionException extends RuntimeException {

	public InvalidTransactionException(String message) {
		super(message);
	}
}
