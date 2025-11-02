package com.hacisimsek.inventory;

import com.hacisimsek.inventory.dto.InventoryResponse;
import com.hacisimsek.inventory.service.InventoryService;
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
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@AutoConfigureMockMvc
class InventorySecurityTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtTokenIssuer issuer;

    @MockBean
    InventoryService inventoryService;

    @MockBean
    com.hacisimsek.inventory.messaging.OrderEventsListener orderEventsListener;

    @MockBean
    com.hacisimsek.inventory.service.IdempotencyService idempotencyService;

    @MockBean
    com.hacisimsek.inventory.repository.MessageLogRepository messageLogRepository;

    @Test
    void inventoryReadWithoutTokenShouldFail() throws Exception {
        mvc.perform(get("/inventory/ABC-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inventoryReadWithProperRoleSucceeds() throws Exception {
        when(inventoryService.get("ABC-1")).thenReturn(response());

        mvc.perform(get("/inventory/ABC-1")
                        .header("Authorization", bearer(Roles.INVENTORY_READ)))
                .andExpect(status().isOk());
    }

    @Test
    void opsReplayRequiresOpsRole() throws Exception {
        mvc.perform(post("/ops/replay/dev.inventory.order-created.dlq")
                        .header("Authorization", bearer(Roles.INVENTORY_WRITE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adjustRequiresWriteRole() throws Exception {
        when(inventoryService.adjust(any(), any())).thenReturn(response());

        mvc.perform(put("/inventory/ABC-1/adjust")
                        .header("Authorization", bearer(Roles.INVENTORY_WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"delta":1,"reason":"test"}
                                """))
                .andExpect(status().isOk());
    }

    private String bearer(String... roles) {
        return "Bearer " + issuer.issue("tester@rtos.local", List.of(roles), Duration.ofMinutes(10));
    }

    private InventoryResponse response() {
        return new InventoryResponse("ABC-1", 10, 0, OffsetDateTime.now());
    }
}
