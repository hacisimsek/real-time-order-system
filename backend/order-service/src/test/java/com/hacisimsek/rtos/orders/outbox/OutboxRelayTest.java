package com.hacisimsek.rtos.orders.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacisimsek.rtos.orders.config.OutboxProps;
import com.hacisimsek.rtos.orders.domain.OutboxEvent;
import com.hacisimsek.rtos.orders.domain.OutboxStatus;
import com.hacisimsek.rtos.orders.repository.OutboxEventRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import com.hacisimsek.rtos.orders.metrics.OutboxMetrics;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxEventRepository repository;

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    OutboxMetrics outboxMetrics;

    OutboxProps props;
    ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        props = new OutboxProps();
        props.setBatchSize(10);
        props.setMaxAttempts(3);
        mapper = new ObjectMapper();
    }

    @Test
    void publishesPendingEventsAndMarksSent() throws Exception {
        var payload = mapper.writeValueAsString(Map.of("foo", "bar"));
        var event = new OutboxEvent("evt-1", 1L, "ORDER", "order.created.v1", "order.events", "order.created.v1", payload);
        when(repository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));

        var relay = new OutboxRelay(repository, rabbitTemplate, mapper, props, outboxMetrics);
        relay.publishPending();

        verify(rabbitTemplate).convertAndSend(
                eq("order.events"),
                eq("order.created.v1"),
                eq(Map.of("foo", "bar")),
                any(MessagePostProcessor.class));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(outboxMetrics).recordDispatched("order.created.v1");
        verify(outboxMetrics).refreshPendingCount();
    }

    @Test
    void incrementsAttemptsOnFailure() throws Exception {
        var payload = mapper.writeValueAsString(Map.of("foo", "bar"));
        var event = new OutboxEvent("evt-2", 2L, "ORDER", "order.created.v1", "order.events", "order.created.v1", payload);
        when(repository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("boom"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));

        var relay = new OutboxRelay(repository, rabbitTemplate, mapper, props, outboxMetrics);
        relay.publishPending();

        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        verify(rabbitTemplate, times(1))
                .convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));
        verify(outboxMetrics).recordFailed("order.created.v1");
        verify(outboxMetrics, times(1)).refreshPendingCount();
    }
}
