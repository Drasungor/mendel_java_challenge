package com.mendel.challenge.service;

import com.mendel.challenge.dto.TransactionRequest;
import com.mendel.challenge.dto.TransactionResponse;
import com.mendel.challenge.exception.ResourceNotFoundException;
import com.mendel.challenge.model.Transaction;
import com.mendel.challenge.repository.TransactionRepository;
import java.util.List;
import java.util.Optional;
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

	/**
	 * PUT semantics: creates the transaction with the given id if it doesn't exist,
	 * otherwise replaces all of its fields with the request data.
	 */
	@Transactional
	public UpsertResult upsert(Long id, TransactionRequest request) {
		Transaction parent = request.parentId() == null ? null : getOrThrow(request.parentId());

		Optional<Transaction> existing = transactionRepository.findById(id);
		if (existing.isPresent()) {
			Transaction transaction = existing.get();
			transaction.setAmount(request.amount());
			transaction.setType(request.type());
			transaction.setParent(parent);
			// No save() needed: the entity is managed, so Hibernate writes the changes
			// (UPDATE) automatically when the database transaction commits ("dirty checking").
			return new UpsertResult(TransactionResponse.from(transaction), false);
		}

		Transaction created = transactionRepository.save(
				new Transaction(id, request.amount(), request.type(), parent));
		return new UpsertResult(TransactionResponse.from(created), true);
	}

	/** Result of {@link #upsert}: the stored transaction, and whether it was newly created. */
	public record UpsertResult(TransactionResponse transaction, boolean created) {
	}

	private Transaction getOrThrow(Long id) {
		return transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction " + id + " not found"));
	}
}
