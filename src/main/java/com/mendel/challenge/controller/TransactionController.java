package com.mendel.challenge.controller;

import com.mendel.challenge.dto.TransactionRequest;
import com.mendel.challenge.dto.TransactionResponse;
import com.mendel.challenge.service.TransactionService;
import com.mendel.challenge.service.TransactionService.UpsertResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

	/**
	 * Creates (201 Created) or replaces (200 OK) the transaction with the given id.
	 * A negative id is rejected with 400 (Spring validates constraint annotations on
	 * controller parameters automatically).
	 */
	@PutMapping("/{id}")
	public ResponseEntity<TransactionResponse> upsert(@PathVariable @PositiveOrZero Long id,
			@Valid @RequestBody TransactionRequest request) {
		UpsertResult result = transactionService.upsert(id, request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.transaction());
	}
}
