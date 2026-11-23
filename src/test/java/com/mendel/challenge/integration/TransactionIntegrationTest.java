package com.mendel.challenge.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mendel.challenge.controller.TransactionController;
import com.mendel.challenge.model.Transaction;
import com.mendel.challenge.repository.TransactionRepository;
import com.mendel.challenge.service.TransactionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Controller ↔ service integration test.
 *
 * Starts Spring MVC with the real {@link TransactionController}, the real
 * {@link TransactionService} and the real exception handler; only the repository is a
 * Mockito mock. Each test sends an HTTP request and checks the HTTP response, so it covers
 * the whole path in between: request parsing and validation, request → DTO conversion,
 * the service's logic, model → DTO → response conversion, and exception → status mapping.
 *
 * Compared to the other tests:
 * - TransactionControllerTest mocks the service (controller only).
 * - TransactionServiceTest calls the service directly (no HTTP).
 * - TransactionRepositoryTest tests the real in-memory repository.
 */
@WebMvcTest(TransactionController.class)
@Import(TransactionService.class) // @WebMvcTest loads only web components; add the real service
class TransactionIntegrationTest {

	private static final String BASE = "/api/transactions";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TransactionRepository transactionRepository;

	// ---------- GET /api/transactions ----------

	@Test
	void findAllReturnsAllTransactionsWithParentIds() {
		Transaction root = new Transaction(10L, 5000f, "cars", null);
		Transaction child = new Transaction(11L, 10000.5f, "shopping", root);
		when(transactionRepository.findAll()).thenReturn(List.of(root, child));

		MvcTestResult result = mvc.get().uri(BASE).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				[
				  {"id": 10, "amount": 5000.0, "type": "cars", "parentId": null},
				  {"id": 11, "amount": 10000.5, "type": "shopping", "parentId": 10}
				]
				""");
	}

	// ---------- GET /api/transactions/{id} ----------

	@Test
	void findByIdReturnsTransaction() {
		Transaction parent = new Transaction(10L, 5000f, "cars", null);
		when(transactionRepository.findById(11L))
				.thenReturn(Optional.of(new Transaction(11L, 10000.5f, "shopping", parent)));

		MvcTestResult result = mvc.get().uri(BASE + "/11").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"id": 11, "amount": 10000.5, "type": "shopping", "parentId": 10}
				""");
	}

	@Test
	void findByIdReturns404WhenTransactionDoesNotExist() {
		when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThat(mvc.get().uri(BASE + "/99"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.detail").isEqualTo("Transaction 99 not found");
	}

	// ---------- GET /api/transactions/types/{type} ----------

	@Test
	void findIdsByTypeReturnsIds() {
		when(transactionRepository.findIdsByType("cars")).thenReturn(List.of(10L, 12L));

		MvcTestResult result = mvc.get().uri(BASE + "/types/cars").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"ids": [10, 12]}
				""");
	}

	@Test
	void findIdsByTypeReturnsEmptyIdsForUnknownType() {
		when(transactionRepository.findIdsByType("unknown")).thenReturn(List.of());

		MvcTestResult result = mvc.get().uri(BASE + "/types/unknown").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"ids": []}
				""");
	}

	// ---------- GET /api/transactions/sum/{id} ----------

	@Test
	void sumChildrenAmountsReturnsSum() {
		when(transactionRepository.existsById(10L)).thenReturn(true);
		when(transactionRepository.sumAmountsByParentId(10L)).thenReturn(15000.5);

		MvcTestResult result = mvc.get().uri(BASE + "/sum/10").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"sum": 15000.5}
				""");
	}

	@Test
	void sumChildrenAmountsReturns404WhenTransactionDoesNotExist() {
		when(transactionRepository.existsById(99L)).thenReturn(false);

		assertThat(mvc.get().uri(BASE + "/sum/99"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.detail").isEqualTo("Transaction 99 not found");
		verify(transactionRepository, never()).sumAmountsByParentId(any());
	}

	// ---------- PUT /api/transactions/{id} ----------

	@Test
	void upsertCreatesTransactionAndReturns201() {
		Transaction parent = new Transaction(10L, 5000f, "cars", null);
		when(transactionRepository.findById(10L)).thenReturn(Optional.of(parent));
		when(transactionRepository.findById(11L)).thenReturn(Optional.empty());
		// The mocked save() returns the transaction it was given, like the real save()
		when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		MvcTestResult result = put("/11", """
				{"amount": 10000.5, "type": "shopping", "parentId": 10}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"id": 11, "amount": 10000.5, "type": "shopping", "parentId": 10}
				""");
		// The transaction built from the path id + request body is what reaches the repository
		ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(11L);
		assertThat(saved.getValue().getAmount()).isEqualTo(10000.5f);
		assertThat(saved.getValue().getType()).isEqualTo("shopping");
		assertThat(saved.getValue().getParent()).isSameAs(parent);
	}

	@Test
	void upsertReplacesExistingTransactionAndReturns200() {
		Transaction existing = new Transaction(10L, 5000f, "cars", null);
		when(transactionRepository.findById(10L)).thenReturn(Optional.of(existing));

		MvcTestResult result = put("/10", """
				{"amount": 7500, "type": "shopping"}
				""");

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"id": 10, "amount": 7500.0, "type": "shopping", "parentId": null}
				""");
		assertThat(existing.getAmount()).isEqualTo(7500f);
		assertThat(existing.getType()).isEqualTo("shopping");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void upsertReturns404WhenParentDoesNotExist() {
		when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThat(put("/11", """
				{"amount": 100, "type": "cars", "parentId": 99}
				"""))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.detail").isEqualTo("Transaction 99 not found");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void upsertReturns400WhenTransactionIsItsOwnParent() {
		assertThat(put("/10", """
				{"amount": 100, "type": "cars", "parentId": 10}
				"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.detail").isEqualTo("Transaction 10 cannot be its own parent");
		verifyNoInteractions(transactionRepository);
	}

	@Test
	void upsertReturns400WithoutReachingRepositoryWhenBodyIsInvalid() {
		assertThat(put("/10", """
				{"type": ""}
				"""))
				.hasStatus(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(transactionRepository);
	}

	private MvcTestResult put(String path, String body) {
		return mvc.put().uri(BASE + path)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}
}
