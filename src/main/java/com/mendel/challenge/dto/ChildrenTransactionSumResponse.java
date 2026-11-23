package com.mendel.challenge.dto;

/**
 * HTTP response body of GET /api/transactions/sum/{id}: the sum of the amounts of the
 * transaction's direct children, as {"sum": 15000.0}. Used by the controller only.
 */
public record ChildrenTransactionSumResponse(float sum) {

	public static ChildrenTransactionSumResponse from(float sum) {
		return new ChildrenTransactionSumResponse(sum);
	}
}
