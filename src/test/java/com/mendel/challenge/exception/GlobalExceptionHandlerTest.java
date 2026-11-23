package com.mendel.challenge.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web-layer test: starts only Spring MVC + the exception handler,
 * plus a test-only controller that fails on purpose.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.FailingController.class)
@Import(GlobalExceptionHandlerTest.FailingController.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

	private static final String SECRET = "secret-internal-details";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void unexpectedExceptionReturnsGeneric500WithoutLeakingDetails(CapturedOutput output) throws Exception {
		MvcTestResult result = mvc.get().uri("/test/unexpected").exchange();

		// Client sees a generic problem+json response with an errorId...
		assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("An unexpected error occurred.");
		assertThat(result).bodyJson().extractingPath("$.errorId").isNotNull();
		// ...but nothing about the actual exception
		assertThat(result).bodyText()
				.doesNotContain(SECRET)
				.doesNotContain("IllegalStateException")
				.doesNotContain("FailingController");

		// The server log has the full details, tagged with the same errorId
		String errorId = JsonPath.read(result.getResponse().getContentAsString(), "$.errorId");
		assertThat(output.getOut())
				.contains("errorId=" + errorId)
				.contains("GET /test/unexpected")
				.contains("java.lang.IllegalStateException: " + SECRET)
				.contains("at com.mendel.challenge.exception.GlobalExceptionHandlerTest$FailingController");
	}

	@Test
	void specificHandlersStillWinOverTheCatchAll() {
		assertThat(mvc.get().uri("/test/not-found"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.detail").isEqualTo("Thing 42 not found");
	}

	@RestController
	static class FailingController {

		@GetMapping("/test/unexpected")
		String unexpected() {
			throw new IllegalStateException(SECRET);
		}

		@GetMapping("/test/not-found")
		String notFound() {
			throw new ResourceNotFoundException("Thing 42 not found");
		}
	}
}
