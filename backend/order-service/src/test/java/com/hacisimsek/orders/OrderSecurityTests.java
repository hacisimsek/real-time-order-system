package com.hacisimsek.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacisimsek.orders.domain.Order;
import com.hacisimsek.orders.domain.OrderStatus;
import com.hacisimsek.orders.dto.OrderCreateRequest;
import com.hacisimsek.orders.service.OrderService;
import com.hacisimsek.security.JwtTokenIssuer;
import com.hacisimsek.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@AutoConfigureMockMvc
class OrderSecurityTests {

    static {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtTokenIssuer issuer;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OrderService orderService;

    @MockBean
    com.hacisimsek.orders.repository.OutboxEventRepository outboxEventRepository;

    @Test
    void createOrderWithoutTokenShouldFail() throws Exception {
        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrderWithWriterRoleSucceeds() throws Exception {
        when(orderService.create(any())).thenReturn(sampleOrder());
        String token = bearer(Roles.ORDER_WRITE);
        var payload = new OrderCreateRequest("C-1", 1000L, "TRY", List.of(new OrderCreateRequest.Item("SKU", 1)));

        mvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatusWithoutWriterRoleForbidden() throws Exception {
        when(orderService.changeStatus(any(), any())).thenReturn(sampleOrder());
        String token = bearer(Roles.ORDER_READ);

        mvc.perform(patch("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", token)
                        .content("{\"status\":\"FULFILLED\"}"))
                .andExpect(status().isForbidden());
    }

    private Order sampleOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId("C-1");
        order.setAmountCents(1000L);
        order.setCurrency("TRY");
        order.setStatus(OrderStatus.CREATED);
        return order;
    }

    private String bearer(String... roles) {
        String token = issuer.issue("tester@rtos.local", List.of(roles), Duration.ofMinutes(10));
        return "Bearer " + token;
    }
}
