package com.mendel.challenge.service;

import com.mendel.challenge.dto.TransactionDto;
import com.mendel.challenge.dto.UpsertResult;
import com.mendel.challenge.exception.ResourceNotFoundException;
import com.mendel.challenge.model.Transaction;
import com.mendel.challenge.repository.TransactionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: business logic and (database) transaction boundaries.
 * Works only with {@link TransactionDto}; it knows nothing about the HTTP request/response types.
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

	public List<TransactionDto> findAll() {
		return transactionRepository.findAll().stream().map(TransactionDto::from).toList();
	}

	public TransactionDto findById(Long id) {
		return TransactionDto.from(getOrThrow(id));
	}

	/**
	 * Ids of the transactions with the given type. Types are free-form (there is no list of
	 * valid types), so an unknown type is not an error: it simply returns an empty list.
	 */
	public List<Long> findIdsByType(String type) {
		return transactionRepository.findIdsByType(type);
	}

	/**
	 * PUT semantics: creates the transaction with the given id if it doesn't exist,
	 * otherwise replaces all of its fields with the request data.
	 */
	@Transactional
	public UpsertResult upsert(TransactionDto data) {
		Transaction parent = data.parentId() == null ? null : getOrThrow(data.parentId());

		Optional<Transaction> existing = transactionRepository.findById(data.id());
		if (existing.isPresent()) {
			Transaction transaction = existing.get();
			transaction.setAmount(data.amount());
			transaction.setType(data.type());
			transaction.setParent(parent);
			// No save() needed: the entity is managed, so Hibernate writes the changes
			// (UPDATE) automatically when the database transaction commits ("dirty checking").
			return new UpsertResult(TransactionDto.from(transaction), false);
		}

		Transaction created = transactionRepository.save(
				new Transaction(data.id(), data.amount(), data.type(), parent));
		return new UpsertResult(TransactionDto.from(created), true);
	}

	private Transaction getOrThrow(Long id) {
		return transactionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction " + id + " not found"));
	}
}
