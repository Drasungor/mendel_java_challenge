package com.mendel.challenge.dto;

import java.util.List;

/**
 * HTTP response body of GET /api/transactions/types/{type}: the ids of the transactions
 * with that type, as {"ids": [10, 11]}. Used by the controller only.
 */
public record TransactionTypeListResponse(List<Long> ids) {

	public static TransactionTypeListResponse from(List<Long> ids) {
		return new TransactionTypeListResponse(ids);
	}
}
