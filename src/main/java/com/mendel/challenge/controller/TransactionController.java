package com.mendel.challenge.controller;

import com.mendel.challenge.dto.TransactionRequest;
import com.mendel.challenge.dto.TransactionResponse;
import com.mendel.challenge.service.TransactionService;
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
@RequestMapping("/api/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@GetMapping
	public List<TransactionResponse> findAll() {
		return transactionService.findAll();
	}

	@GetMapping("/{id}")
	public TransactionResponse findById(@PathVariable Long id) {
		return transactionService.findById(id);
	}

	@PostMapping
	public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
		TransactionResponse created = transactionService.create(request);
		return ResponseEntity.created(URI.create("/api/transactions/" + created.id())).body(created);
	}
}
