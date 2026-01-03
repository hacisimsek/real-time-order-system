package com.hacisimsek.rtos.notification;

import com.hacisimsek.rtos.security.JwtTokenIssuer;
import com.hacisimsek.rtos.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false"
})
@AutoConfigureMockMvc
class NotificationSecurityTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtTokenIssuer issuer;

    @MockBean
    com.hacisimsek.rtos.notification.messaging.OrderEventsListener orderEventsListener;

    @TestConfiguration
    static class Stubs {
        @Bean
        org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory() {
            var factory = mock(org.springframework.amqp.rabbit.connection.ConnectionFactory.class);
            when(factory.createConnection()).thenReturn(mock(org.springframework.amqp.rabbit.connection.Connection.class));
            return factory;
        }
    }

    @Test
    void actuatorHealthRemainsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void otherEndpointsRequireToken() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/")
                        .header("Authorization", bearer(Roles.NOTIFICATION_READ)))
                .andExpect(status().isNotFound());
    }

    private String bearer(String... roles) {
        return "Bearer " + issuer.issue("tester@rtos.local", List.of(roles), Duration.ofMinutes(5));
    }
}
