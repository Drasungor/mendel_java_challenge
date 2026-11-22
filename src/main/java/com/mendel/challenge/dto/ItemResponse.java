package com.mendel.challenge.dto;

import com.mendel.challenge.model.Item;

/** Response body returned to API clients. Keeps JPA entities out of the HTTP layer. */
public record ItemResponse(Long id, String name) {

	public static ItemResponse from(Item item) {
		return new ItemResponse(item.getId(), item.getName());
	}
}
