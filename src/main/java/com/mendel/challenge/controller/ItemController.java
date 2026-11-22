package com.mendel.challenge.controller;

import com.mendel.challenge.dto.ItemRequest;
import com.mendel.challenge.dto.ItemResponse;
import com.mendel.challenge.service.ItemService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller layer: HTTP mapping and input validation only. All logic is delegated
 * to the service, which Spring injects through the constructor.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

	private final ItemService itemService;

	public ItemController(ItemService itemService) {
		this.itemService = itemService;
	}

	@GetMapping
	public List<ItemResponse> findAll() {
		return itemService.findAll();
	}

	@GetMapping("/{id}")
	public ItemResponse findById(@PathVariable Long id) {
		return itemService.findById(id);
	}

	@PostMapping
	public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
		ItemResponse created = itemService.create(request);
		return ResponseEntity.created(URI.create("/api/items/" + created.id())).body(created);
	}
}
