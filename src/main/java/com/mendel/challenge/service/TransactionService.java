package com.mendel.challenge.service;

import com.mendel.challenge.dto.TransactionRequest;
import com.mendel.challenge.dto.TransactionResponse;
import com.mendel.challenge.exception.ResourceNotFoundException;
import com.mendel.challenge.model.Transaction;
import com.mendel.challenge.repository.TransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: business logic and (database) transaction boundaries.
 *
 * Note: {@code @Transactional} is Spring's database-transaction annotation and is unrelated
 * to the {@link Transaction} entity.
 */
@Service
@Transactional(readOnly = true)
public class TransactionService {

	private final TransactionRepository transactionRepository;

	public TransactionService(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	public List<TransactionResponse> findAll() {
		return transactionRepository.findAll().stream().map(TransactionResponse::from).toList();
	}

	public TransactionResponse findById(Long id) {
		return TransactionResponse.from(getOrThrow(id));
	}

	@Transactional
	public TransactionResponse create(TransactionRequest request) {
		Transaction parent = request.parentId() == null ? null : getOrThrow(request.parentId());
		Transaction saved = transactionRepository.save(new Transaction(request.amount(), request.type(), parent));
		return TransactionResponse.from(saved);
	}

	private Transaction getOrThrow(Long id) {
		return transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction " + id + " not found"));
	}
}
