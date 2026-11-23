package com.mendel.challenge.dto;

import com.mendel.challenge.model.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body of PUT /api/transactions/{id} (create or replace):
 * {"amount": 10.5, "type": "cars", "parentId": 123}. Used by the controller only.
 *
 * - {@code amount}: required. A boxed Float so a missing value arrives as null and fails
 *   @NotNull (a primitive float would silently default to 0).
 * - {@code type}: required, not blank, at most {@link Transaction#TYPE_MAX_LENGTH} characters
 *   (the limit defined by the model).
 * - {@code parentId}: optional.
 */
public record TransactionRequest(
		@NotNull Float amount,
		@NotBlank @Size(max = Transaction.TYPE_MAX_LENGTH) String type,
		Long parentId) {

	/** Converts this request into the service-layer type. The id comes from the URL path. */
	public TransactionDto toDto(Long id) {
		return new TransactionDto(id, amount, type, parentId);
	}
}
