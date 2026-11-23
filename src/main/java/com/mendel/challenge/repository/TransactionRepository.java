package com.mendel.challenge.repository;

import com.mendel.challenge.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Data layer: Spring Data generates the implementation at runtime
 * (save, findById, findAll, existsById, deleteById, ...).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	/**
	 * Ids of all transactions with exactly this type (case-sensitive), in ascending order.
	 * Selects only the id column instead of loading whole entities; the WHERE clause uses
	 * the idx_transactions_type index. Returns an empty list if no transaction has the type.
	 */
	@Query("SELECT t.id FROM Transaction t WHERE t.type = :type ORDER BY t.id")
	List<Long> findIdsByType(String type);

	/**
	 * Sum of the amounts of the direct children of the given transaction, computed by the
	 * database: the WHERE clause first narrows the rows to that parent's children (using the
	 * idx_transactions_parent_id index), then SUM aggregates only those rows. A single total
	 * needs no GROUP BY, and only one number travels back instead of every child row.
	 * COALESCE turns the SQL result for "no rows" (NULL) into 0.
	 */
	@Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t WHERE t.parent.id = :parentId")
	double sumAmountsByParentId(Long parentId);
}
