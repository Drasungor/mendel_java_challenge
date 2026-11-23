package com.mendel.challenge.dto;

/**
 * HTTP response body: {"id": 123, "amount": 10.5, "type": "cars", "parentId": null}.
 * Used by the controller only; built from the service-layer {@link TransactionDto}.
 */
public record TransactionResponse(Long id, float amount, String type, Long parentId) {

	public static TransactionResponse from(TransactionDto transaction) {
		return new TransactionResponse(
				transaction.id(),
				transaction.amount(),
				transaction.type(),
				transaction.parentId());
	}
}
