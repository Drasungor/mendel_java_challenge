package com.mendel.challenge.dto;

import com.mendel.challenge.model.Transaction;

/**
 * Service-layer representation of a transaction: the only transaction type that crosses
 * the controller ↔ service boundary, in both directions.
 *
 * The service never sees the HTTP types ({@link TransactionRequest}, {@link TransactionResponse});
 * the controller converts them to and from this record.
 */
public record TransactionDto(Long id, float amount, String type, Long parentId) {

	public static TransactionDto from(Transaction transaction) {
		Transaction parent = transaction.getParent();
		return new TransactionDto(
				transaction.getId(),
				transaction.getAmount(),
				transaction.getType(),
				parent == null ? null : parent.getId());
	}
}
