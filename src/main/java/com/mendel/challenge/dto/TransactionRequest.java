package com.mendel.challenge.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a transaction: {"amount": 10.5, "parentId": 123}.
 * {@code amount} is a boxed Float so a missing value arrives as null and fails @NotNull
 * (a primitive float would silently default to 0). {@code parentId} is optional.
 */
public record TransactionRequest(@NotNull Float amount, Long parentId) {
}
