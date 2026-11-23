package com.mendel.challenge.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mendel.challenge.dto.ChildrenTransactionSumResponse;
import com.mendel.challenge.dto.TransactionDto;
import com.mendel.challenge.dto.TransactionResponse;
import com.mendel.challenge.dto.TransactionTypeListResponse;
import com.mendel.challenge.dto.UpsertResult;
import com.mendel.challenge.exception.ResourceNotFoundException;
import com.mendel.challenge.service.TransactionService;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Web-layer test (no database): starts only Spring MVC, the controller and the exception
 * handler. The service is a Mockito mock, so each test controls exactly what the service
 * returns and checks only how the controller maps it to HTTP.
 *
 * Each successful response is checked two ways:
 * - {@code isStrictlyEqualTo}: the JSON has exactly the fields of the response DTO
 *   (no missing or extra fields), with the expected values.
 * - {@code convertTo}: the body deserializes into the response DTO and equals the expected one.
 */
@WebMvcTest(TransactionController.class)
@ActiveProfiles("test")
class TransactionControllerTest {

	private static final String BASE = "/api/transactions";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private TransactionService transactionService;

	// ---------- GET /api/transactions ----------

	@Test
	void findAllReturnsListOfTransactionResponses() {
		when(transactionService.findAll()).thenReturn(List.of(
				new TransactionDto(10L, 5000f, "cars", null),
				new TransactionDto(11L, 10000.5f, "shopping", 10L)));

		MvcTestResult result = mvc.get().uri(BASE).exchange();

		assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				[
				  {"id": 10, "amount": 5000.0, "type": "cars", "parentId": null},
				  {"id": 11, "amount": 10000.5, "type": "shopping", "parentId": 10}
				]
				""");
		assertThat(result).bodyJson().convertTo(InstanceOfAssertFactories.array(TransactionResponse[].class)).containsExactly(
				new TransactionResponse(10L, 5000f, "cars", null),
				new TransactionResponse(11L, 10000.5f, "shopping", 10L));
	}

	@Test
	void findAllReturnsEmptyArrayWhenThereAreNoTransactions() {
		when(transactionService.findAll()).thenReturn(List.of());

		MvcTestResult result = mvc.get().uri(BASE).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("[]");
	}

	// ---------- GET /api/transactions/{id} ----------

	@Test
	void findByIdReturnsTransactionResponse() {
		when(transactionService.findById(11L)).thenReturn(new TransactionDto(11L, 10000.5f, "shopping", 10L));

		MvcTestResult result = mvc.get().uri(BASE + "/11").exchange();

		assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"id": 11, "amount": 10000.5, "type": "shopping", "parentId": 10}
				""");
		assertThat(result).bodyJson().convertTo(TransactionResponse.class)
				.isEqualTo(new TransactionResponse(11L, 10000.5f, "shopping", 10L));
	}

	@Test
	void findByIdReturns404WhenServiceThrowsNotFound() {
		when(transactionService.findById(99L)).thenThrow(new ResourceNotFoundException("Transaction 99 not found"));

		assertThat(mvc.get().uri(BASE + "/99"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.detail").isEqualTo("Transaction 99 not found");
	}

	// ---------- GET /api/transactions/types/{type} ----------

	@Test
	void findIdsByTypeReturnsTransactionTypeListResponse() {
		when(transactionService.findIdsByType("cars")).thenReturn(List.of(10L, 12L));

		MvcTestResult result = mvc.get().uri(BASE + "/types/cars").exchange();

		assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"ids": [10, 12]}
				""");
		assertThat(result).bodyJson().convertTo(TransactionTypeListResponse.class)
				.isEqualTo(new TransactionTypeListResponse(List.of(10L, 12L)));
	}

	@Test
	void findIdsByTypeReturnsEmptyIdsWhenTypeIsUnknown() {
		when(transactionService.findIdsByType("unknown")).thenReturn(List.of());

		MvcTestResult result = mvc.get().uri(BASE + "/types/unknown").exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"ids": []}
				""");
		assertThat(result).bodyJson().convertTo(TransactionTypeListResponse.class)
				.isEqualTo(new TransactionTypeListResponse(List.of()));
	}

	// ---------- GET /api/transactions/sum/{id} ----------

	@Test
	void sumChildrenAmountsReturnsChildrenTransactionSumResponse() {
		when(transactionService.sumChildrenAmounts(10L)).thenReturn(15000.5f);

		MvcTestResult result = mvc.get().uri(BASE + "/sum/10").exchange();

		assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"sum": 15000.5}
				""");
		assertThat(result).bodyJson().convertTo(ChildrenTransactionSumResponse.class)
				.isEqualTo(new ChildrenTransactionSumResponse(15000.5f));
	}

	@Test
	void sumChildrenAmountsReturns404WhenServiceThrowsNotFound() {
		when(transactionService.sumChildrenAmounts(99L))
				.thenThrow(new ResourceNotFoundException("Transaction 99 not found"));

		assertThat(mvc.get().uri(BASE + "/sum/99"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.detail").isEqualTo("Transaction 99 not found");
	}

	// ---------- PUT /api/transactions/{id} ----------

	@Test
	void upsertReturns201AndTransactionResponseWhenCreated() {
		TransactionDto stored = new TransactionDto(11L, 10000.5f, "shopping", 10L);
		when(transactionService.upsert(any())).thenReturn(new UpsertResult(stored, true));

		MvcTestResult result = mvc.put().uri(BASE + "/11")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 10000.5, "type": "shopping", "parentId": 10}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"id": 11, "amount": 10000.5, "type": "shopping", "parentId": 10}
				""");
		assertThat(result).bodyJson().convertTo(TransactionResponse.class)
				.isEqualTo(new TransactionResponse(11L, 10000.5f, "shopping", 10L));
		// The id comes from the path, the rest from the body
		verify(transactionService).upsert(new TransactionDto(11L, 10000.5f, "shopping", 10L));
	}

	@Test
	void upsertReturns200AndTransactionResponseWhenReplaced() {
		TransactionDto stored = new TransactionDto(10L, 5000f, "cars", null);
		when(transactionService.upsert(any())).thenReturn(new UpsertResult(stored, false));

		MvcTestResult result = mvc.put().uri(BASE + "/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().isStrictlyEqualTo("""
				{"id": 10, "amount": 5000.0, "type": "cars", "parentId": null}
				""");
		assertThat(result).bodyJson().convertTo(TransactionResponse.class)
				.isEqualTo(new TransactionResponse(10L, 5000f, "cars", null));
		verify(transactionService).upsert(new TransactionDto(10L, 5000f, "cars", null));
	}

	@Test
	void upsertReturns400WithoutCallingServiceWhenBodyIsInvalid() {
		assertThat(mvc.put().uri(BASE + "/10")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"type": ""}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);

		verifyNoInteractions(transactionService);
	}

	@Test
	void upsertReturns400WithoutCallingServiceWhenIdIsNegative() {
		assertThat(mvc.put().uri(BASE + "/-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount": 5000, "type": "cars"}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST);

		verifyNoInteractions(transactionService);
	}
}
