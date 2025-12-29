package com.hacisimsek.notification;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires external infra (RabbitMQ); covered by slice tests in this module")
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
