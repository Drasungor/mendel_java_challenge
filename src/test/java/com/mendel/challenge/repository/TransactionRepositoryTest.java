package com.mendel.challenge.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mendel.challenge.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Data-layer test: runs the real JPQL queries against the in-memory H2 database.
 * Each test runs in its own database transaction that is rolled back at the end,
 * so tests never see each other's rows.
 *
 * This is where the custom queries are tested. The service tests mock the repository,
 * so they can't check what these queries actually compute.
 */
@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private TransactionRepository transactionRepository;

	// ---------- sumAmountsByParentId ----------

	@Test
	void sumAmountsByParentIdAddsTheAmountsOfAllDirectChildren() {
		Transaction parent = persist(new Transaction(10L, 5000f, "cars", null));
		persist(new Transaction(11L, 10000.5f, "shopping", parent));
		persist(new Transaction(12L, 2.25f, "shopping", parent));
		persist(new Transaction(13L, 300f, "food", parent));

		assertThat(transactionRepository.sumAmountsByParentId(10L)).isEqualTo(10302.75);
	}

	@Test
	void sumAmountsByParentIdIgnoresGrandchildrenAndOtherParentsChildren() {
		Transaction parent = persist(new Transaction(10L, 5000f, "cars", null));
		Transaction child = persist(new Transaction(11L, 100f, "shopping", parent));
		persist(new Transaction(12L, 200f, "shopping", parent));
		persist(new Transaction(13L, 999f, "food", child)); // grandchild of 10: not counted
		Transaction otherParent = persist(new Transaction(20L, 1f, "cars", null));
		persist(new Transaction(21L, 888f, "food", otherParent)); // child of another parent: not counted

		assertThat(transactionRepository.sumAmountsByParentId(10L)).isEqualTo(300.0);
	}

	@Test
	void sumAmountsByParentIdReturnsZeroWhenTransactionHasNoChildren() {
		persist(new Transaction(10L, 5000f, "cars", null));

		assertThat(transactionRepository.sumAmountsByParentId(10L)).isZero();
	}

	private Transaction persist(Transaction transaction) {
		return entityManager.persistAndFlush(transaction);
	}
}
