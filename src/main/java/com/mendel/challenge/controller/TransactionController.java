package com.mendel.challenge.controller;

import com.mendel.challenge.dto.TransactionRequest;
import com.mendel.challenge.dto.TransactionResponse;
import com.mendel.challenge.dto.TransactionTypeListResponse;
import com.mendel.challenge.dto.UpsertResult;
import com.mendel.challenge.service.TransactionService;
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
 *
 * It is also the boundary between HTTP types and service types: requests are converted
 * to {@code TransactionDto} before calling the service, and service results are converted
 * to {@link TransactionResponse} before being returned.
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
		return transactionService.findAll().stream().map(TransactionResponse::from).toList();
	}

	@GetMapping("/{id}")
	public TransactionResponse findById(@PathVariable Long id) {
		return TransactionResponse.from(transactionService.findById(id));
	}

	/** Ids of all transactions of the given type; an empty array if there are none. */
	@GetMapping("/types/{type}")
	public TransactionTypeListResponse findIdsByType(@PathVariable String type) {
		return TransactionTypeListResponse.from(transactionService.findIdsByType(type));
	}

	/**
	 * Creates (201 Created) or replaces (200 OK) the transaction with the given id.
	 * A negative id is rejected with 400 (Spring validates constraint annotations on
	 * controller parameters automatically).
	 */
	@PutMapping("/{id}")
	public ResponseEntity<TransactionResponse> upsert(@PathVariable @PositiveOrZero Long id,
			@Valid @RequestBody TransactionRequest request) {
		UpsertResult result = transactionService.upsert(request.toDto(id));
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(TransactionResponse.from(result.transaction()));
	}
}
