package com.hacisimsek.rtos.orders;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires external infra (Postgres/RabbitMQ); covered by slice tests in this module")
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
