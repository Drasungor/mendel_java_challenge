package com.mendel.challenge.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for creating an item. Validated in the controller with @Valid. */
public record ItemRequest(@NotBlank String name) {
}
