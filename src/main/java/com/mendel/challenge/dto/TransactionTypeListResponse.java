package com.mendel.challenge.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

/**
 * HTTP response body of GET /api/transactions/types/{type}: the ids of the transactions
 * with that type. Used by the controller only.
 *
 * {@code @JsonValue} makes Jackson serialize this record as just its list, so the JSON is a
 * plain array: [10, 11]. Without it, the JSON would be an object: {"ids": [10, 11]}.
 */
public record TransactionTypeListResponse(@JsonValue List<Long> ids) {

	public static TransactionTypeListResponse from(List<Long> ids) {
		return new TransactionTypeListResponse(ids);
	}
}
