package com.todongsan.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "jwt.secret=test-secret-key-must-be-at-least-32-chars!!")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
