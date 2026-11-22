package com.mendel.challenge.dto;

import com.mendel.challenge.model.Transaction;

/**
 * Response body: {"id": 123, "amount": 10.5, "type": "cars", "parentId": null}.
 * Keeps JPA entities out of the HTTP layer.
 */
public record TransactionResponse(Long id, float amount, String type, Long parentId) {

	public static TransactionResponse from(Transaction transaction) {
		Transaction parent = transaction.getParent();
		return new TransactionResponse(
				transaction.getId(),
				transaction.getAmount(),
				transaction.getType(),
				parent == null ? null : parent.getId());
	}
}
