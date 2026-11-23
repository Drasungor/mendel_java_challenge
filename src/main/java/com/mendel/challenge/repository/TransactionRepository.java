package com.mendel.challenge.repository;

import com.mendel.challenge.model.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Data layer: in-memory storage of transactions, keyed by id.
 *
 * The map holds one entry per transaction id, and saving a transaction with an existing id
 * replaces the stored one. Lookups by type or parent scan all stored transactions.
 *
 * The stored instances are returned directly (not copies), so changes made to a transaction
 * returned by {@link #findById} are visible to later reads without calling {@link #save}.
 *
 * A {@link ConcurrentHashMap} is used because the singleton bean is shared by all request
 * threads. Individual operations are thread-safe; sequences of operations are not atomic.
 */
@Repository
public class TransactionRepository {

	private final Map<Long, Transaction> transactions = new ConcurrentHashMap<>();

	/** Stores the transaction under its id, replacing any existing one, and returns it. */
	public Transaction save(Transaction transaction) {
		Objects.requireNonNull(transaction, "transaction must not be null");
		Objects.requireNonNull(transaction.getId(), "transaction id must not be null");
		transactions.put(transaction.getId(), transaction);
		return transaction;
	}

	public Optional<Transaction> findById(Long id) {
		return Optional.ofNullable(transactions.get(id));
	}

	public boolean existsById(Long id) {
		return transactions.containsKey(id);
	}

	/** All stored transactions, in no particular order. */
	public List<Transaction> findAll() {
		return new ArrayList<>(transactions.values());
	}

	/**
	 * Ids of all transactions with exactly this type (case-sensitive), in ascending order.
	 * Returns an empty list if no transaction has the type.
	 */
	public List<Long> findIdsByType(String type) {
		return transactions.values().stream()
				.filter(transaction -> Objects.equals(transaction.getType(), type))
				.map(Transaction::getId)
				.sorted()
				.toList();
	}

	/**
	 * Sum of the amounts of the direct children of the given transaction (grandchildren are
	 * not included); 0 if it has none.
	 */
	public double sumAmountsByParentId(Long parentId) {
		return transactions.values().stream()
				.filter(transaction -> transaction.getParent() != null
						&& Objects.equals(transaction.getParent().getId(), parentId))
				.mapToDouble(Transaction::getAmount)
				.sum();
	}
}
