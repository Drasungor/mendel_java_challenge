package com.mendel.challenge.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mendel.challenge.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test of the in-memory repository: no Spring context is needed.
 * Each test gets a fresh repository instance, so tests never see each other's data.
 *
 * This is where the lookup and aggregation logic is tested. The service tests mock the
 * repository, so they can't check what these methods actually compute.
 */
class TransactionRepositoryTest {

	private TransactionRepository transactionRepository;

	@BeforeEach
	void setUp() {
		transactionRepository = new TransactionRepository();
	}

	// ---------- sumAmountsByParentId ----------

	@Test
	void sumAmountsByParentIdAddsTheAmountsOfAllDirectChildren() {
		Transaction parent = save(new Transaction(10L, 5000f, "cars", null));
		save(new Transaction(11L, 10000.5f, "shopping", parent));
		save(new Transaction(12L, 2.25f, "shopping", parent));
		save(new Transaction(13L, 300f, "food", parent));

		assertThat(transactionRepository.sumAmountsByParentId(10L)).isEqualTo(10302.75);
	}

	@Test
	void sumAmountsByParentIdIgnoresGrandchildrenAndOtherParentsChildren() {
		Transaction parent = save(new Transaction(10L, 5000f, "cars", null));
		Transaction child = save(new Transaction(11L, 100f, "shopping", parent));
		save(new Transaction(12L, 200f, "shopping", parent));
		save(new Transaction(13L, 999f, "food", child)); // grandchild of 10: not counted
		Transaction otherParent = save(new Transaction(20L, 1f, "cars", null));
		save(new Transaction(21L, 888f, "food", otherParent)); // child of another parent: not counted

		assertThat(transactionRepository.sumAmountsByParentId(10L)).isEqualTo(300.0);
	}

	@Test
	void sumAmountsByParentIdReturnsZeroWhenTransactionHasNoChildren() {
		save(new Transaction(10L, 5000f, "cars", null));

		assertThat(transactionRepository.sumAmountsByParentId(10L)).isZero();
	}

	private Transaction save(Transaction transaction) {
		return transactionRepository.save(transaction);
	}
}
