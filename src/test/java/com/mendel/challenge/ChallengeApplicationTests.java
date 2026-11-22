package com.mendel.challenge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: the full application context (controllers, services, repositories, DB) starts. */
@SpringBootTest
@ActiveProfiles("test")
class ChallengeApplicationTests {

	@Test
	void contextLoads() {
	}

}
