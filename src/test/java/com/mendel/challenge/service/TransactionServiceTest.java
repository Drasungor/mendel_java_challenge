package com.mendel.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mendel.challenge.dto.TransactionDto;
import com.mendel.challenge.dto.UpsertResult;
import com.mendel.challenge.exception.InvalidTransactionException;
import com.mendel.challenge.exception.ResourceNotFoundException;
import com.mendel.challenge.model.Transaction;
import com.mendel.challenge.repository.TransactionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Plain unit test (no Spring context): the repository is a Mockito mock, so each
 * test controls exactly what the data layer returns and checks only the service's logic.
 *
 * MockitoExtension uses strict stubs: a test fails if it stubs a repository call the service
 * never makes, which keeps the stubbing in each test honest.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private TransactionService transactionService;

	// ---------- findAll ----------

	@Test
	void findAllMapsTransactionsToDtos() {
		Transaction root = new Transaction(10L, 5000f, "cars", null);
		Transaction child = new Transaction(11L, 10000.5f, "shopping", root);
		when(transactionRepository.findAll()).thenReturn(List.of(root, child));

		assertThat(transactionService.findAll()).containsExactly(
				new TransactionDto(10L, 5000f, "cars", null),
				new TransactionDto(11L, 10000.5f, "shopping", 10L));
	}

	@Test
	void findAllReturnsEmptyListWhenThereAreNoTransactions() {
		when(transactionRepository.findAll()).thenReturn(List.of());

		assertThat(transactionService.findAll()).isEmpty();
	}

	// ---------- findById ----------

	@Test
	void findByIdReturnsDtoWithParentId() {
		Transaction parent = new Transaction(10L, 5000f, "cars", null);
		when(transactionRepository.findById(11L))
				.thenReturn(Optional.of(new Transaction(11L, 10000.5f, "shopping", parent)));

		assertThat(transactionService.findById(11L))
				.isEqualTo(new TransactionDto(11L, 10000.5f, "shopping", 10L));
	}

	@Test
	void findByIdThrowsNotFoundWhenTransactionDoesNotExist() {
		when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Transaction 99 not found");
	}

	// ---------- findIdsByType ----------

	@Test
	void findIdsByTypeReturnsRepositoryIds() {
		when(transactionRepository.findIdsByType("cars")).thenReturn(List.of(10L, 12L));

		assertThat(transactionService.findIdsByType("cars")).containsExactly(10L, 12L);
	}

	@Test
	void findIdsByTypeReturnsEmptyListForUnknownType() {
		when(transactionRepository.findIdsByType("unknown")).thenReturn(List.of());

		assertThat(transactionService.findIdsByType("unknown")).isEmpty();
	}

	// ---------- sumChildrenAmounts ----------

	@Test
	void sumChildrenAmountsReturnsRepositorySum() {
		when(transactionRepository.existsById(10L)).thenReturn(true);
		when(transactionRepository.sumAmountsByParentId(10L)).thenReturn(15000.5);

		assertThat(transactionService.sumChildrenAmounts(10L)).isEqualTo(15000.5f);
	}

	@Test
	void sumChildrenAmountsReturnsZeroWhenTransactionHasNoChildren() {
		when(transactionRepository.existsById(10L)).thenReturn(true);
		when(transactionRepository.sumAmountsByParentId(10L)).thenReturn(0.0);

		assertThat(transactionService.sumChildrenAmounts(10L)).isZero();
	}

	@Test
	void sumChildrenAmountsThrowsNotFoundWithoutQueryingSumWhenTransactionDoesNotExist() {
		when(transactionRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> transactionService.sumChildrenAmounts(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Transaction 99 not found");
		verify(transactionRepository, never()).sumAmountsByParentId(any());
	}

	// ---------- upsert: create ----------

	@Test
	void upsertCreatesTransactionWithoutParent() {
		when(transactionRepository.findById(10L)).thenReturn(Optional.empty());
		when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UpsertResult result = transactionService.upsert(new TransactionDto(10L, 5000f, "cars", null));

		assertThat(result).isEqualTo(new UpsertResult(new TransactionDto(10L, 5000f, "cars", null), true));
		Transaction saved = captureSaved();
		assertThat(saved.getId()).isEqualTo(10L);
		assertThat(saved.getAmount()).isEqualTo(5000f);
		assertThat(saved.getType()).isEqualTo("cars");
		assertThat(saved.getParent()).isNull();
	}

	@Test
	void upsertCreatesTransactionLinkedToExistingParent() {
		Transaction parent = new Transaction(10L, 5000f, "cars", null);
		when(transactionRepository.findById(10L)).thenReturn(Optional.of(parent));
		when(transactionRepository.findById(11L)).thenReturn(Optional.empty());
		when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UpsertResult result = transactionService.upsert(new TransactionDto(11L, 10000.5f, "shopping", 10L));

		assertThat(result)
				.isEqualTo(new UpsertResult(new TransactionDto(11L, 10000.5f, "shopping", 10L), true));
		assertThat(captureSaved().getParent()).isSameAs(parent);
	}

	// ---------- upsert: replace ----------

	@Test
	void upsertReplacesAllFieldsOfExistingTransactionWithoutSaving() {
		Transaction oldParent = new Transaction(1L, 1f, "old", null);
		Transaction newParent = new Transaction(2L, 2f, "new", null);
		Transaction existing = new Transaction(10L, 5000f, "cars", oldParent);
		when(transactionRepository.findById(2L)).thenReturn(Optional.of(newParent));
		when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));

		UpsertResult result = transactionService.upsert(new TransactionDto(10L, 7500f, "shopping", 2L));

		assertThat(result).isEqualTo(new UpsertResult(new TransactionDto(10L, 7500f, "shopping", 2L), false));
		// The stored instance itself is modified, so no save() is needed
		assertThat(existing.getAmount()).isEqualTo(7500f);
		assertThat(existing.getType()).isEqualTo("shopping");
		assertThat(existing.getParent()).isSameAs(newParent);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void upsertClearsParentWhenReplacingWithoutParentId() {
		Transaction existing = new Transaction(11L, 10000f, "shopping", new Transaction(10L, 5000f, "cars", null));
		when(transactionRepository.findById(11L)).thenReturn(Optional.of(existing));

		UpsertResult result = transactionService.upsert(new TransactionDto(11L, 10000f, "shopping", null));

		assertThat(result.created()).isFalse();
		assertThat(result.transaction().parentId()).isNull();
		assertThat(existing.getParent()).isNull();
	}

	// ---------- upsert: invalid ----------

	@Test
	void upsertThrowsNotFoundWithoutWritingWhenParentDoesNotExist() {
		when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.upsert(new TransactionDto(11L, 100f, "cars", 99L)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Transaction 99 not found");
		verify(transactionRepository, never()).findById(11L);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void upsertRejectsSelfReferencingParentWithoutTouchingRepository() {
		assertThatThrownBy(() -> transactionService.upsert(new TransactionDto(10L, 100f, "cars", 10L)))
				.isInstanceOf(InvalidTransactionException.class)
				.hasMessage("Transaction 10 cannot be its own parent");
		verifyNoInteractions(transactionRepository);
	}

	private Transaction captureSaved() {
		ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(captor.capture());
		return captor.getValue();
	}
}
