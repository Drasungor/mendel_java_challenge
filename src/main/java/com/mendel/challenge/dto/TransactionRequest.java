package com.mendel.challenge.dto;

import com.mendel.challenge.model.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a transaction: {"amount": 10.5, "type": "cars", "parentId": 123}.
 *
 * - {@code amount}: required. A boxed Float so a missing value arrives as null and fails
 *   @NotNull (a primitive float would silently default to 0).
 * - {@code type}: required, not blank, at most {@link Transaction#TYPE_MAX_LENGTH} characters
 *   (the same limit as the database column).
 * - {@code parentId}: optional.
 */
public record TransactionRequest(
		@NotNull Float amount,
		@NotBlank @Size(max = Transaction.TYPE_MAX_LENGTH) String type,
		Long parentId) {
}
