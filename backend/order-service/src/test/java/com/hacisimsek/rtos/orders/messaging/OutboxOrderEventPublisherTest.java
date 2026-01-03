package com.hacisimsek.rtos.orders.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacisimsek.rtos.orders.config.MessagingProps;
import com.hacisimsek.rtos.orders.domain.OutboxEvent;
import com.hacisimsek.rtos.orders.repository.OutboxEventRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxOrderEventPublisherTest {

    @Mock
    OutboxEventRepository repository;

    private OutboxOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        var messagingProps = new MessagingProps(
                "order.events",
                new MessagingProps.Queues("dev.notifications.order-created"),
                new MessagingProps.RoutingKeys("order.created.v1", "order.status-changed.v1"));
        publisher = new OutboxOrderEventPublisher(repository, new ObjectMapper(), messagingProps);
    }

    @Test
    void persistsOrderCreatedPayload() {
        publisher.orderCreated(1L, "C-1", 1500L, "TRY",
                List.of(new OrderEventItem("SKU-1", 2)));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent stored = captor.getValue();

        assertThat(stored.getAggregateId()).isEqualTo(1L);
        assertThat(stored.getEventType()).isEqualTo("order.created.v1");
        assertThat(stored.getRoutingKey()).isEqualTo("order.created.v1");
        assertThat(stored.getPayload()).contains("\"customerId\":\"C-1\"");
    }

    @Test
    void persistsOrderStatusChangedPayload() {
        publisher.orderStatusChanged(2L, com.hacisimsek.rtos.orders.domain.OrderStatus.FULFILLED,
                List.of(new OrderEventItem("SKU-9", 1)));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());

        OutboxEvent stored = captor.getValue();
        assertThat(stored.getAggregateId()).isEqualTo(2L);
        assertThat(stored.getEventType()).isEqualTo("order.status-changed.v1");
        assertThat(stored.getPayload()).contains("\"status\":\"FULFILLED\"");
    }
}
