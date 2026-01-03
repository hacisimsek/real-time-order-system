package com.hacisimsek.rtos.orders.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacisimsek.rtos.orders.config.MessagingProps;
import com.hacisimsek.rtos.orders.domain.OutboxEvent;
import com.hacisimsek.rtos.orders.repository.OutboxEventRepository;
import com.hacisimsek.rtos.orders.domain.OrderStatus;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxOrderEventPublisher.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final MessagingProps messagingProps;

    public OutboxOrderEventPublisher(OutboxEventRepository repository,
                                     ObjectMapper objectMapper,
                                     MessagingProps messagingProps) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.messagingProps = messagingProps;
    }

    @Override
    @Transactional
    public void orderCreated(Long id, String customerId, long amountCents, String currency, java.util.List<OrderEventItem> items) {
        var eventId = UUID.randomUUID().toString();
        var payload = Map.of(
                "type","order.created.v1",
                "eventId", eventId,
                "id", id,
                "customerId", customerId,
                "amountCents", amountCents,
                "currency", currency,
                "items", items.stream().map(it -> Map.of("sku", it.sku(), "qty", it.qty())).toList()
        );
        persist(eventId, id, "ORDER", "order.created.v1",
                messagingProps.exchange(),
                messagingProps.routingKeys().orderCreated(),
                payload);
        log.debug("Enqueued order.created event {}", eventId);
    }

    @Override
    @Transactional
    public void orderStatusChanged(Long id, OrderStatus status, java.util.List<OrderEventItem> items) {
        var eventId = UUID.randomUUID().toString();
        var payload = Map.of(
                "type","order.status-changed.v1",
                "eventId", eventId,
                "id", id,
                "status", status.name(),
                "items", items.stream().map(it -> Map.of("sku", it.sku(), "qty", it.qty())).toList()
        );
        persist(eventId, id, "ORDER", "order.status-changed.v1",
                messagingProps.exchange(),
                messagingProps.routingKeys().orderStatusChanged(),
                payload);
        log.debug("Enqueued order.status-changed event {}", eventId);
    }

    private void persist(String eventId,
                         Long aggregateId,
                         String aggregateType,
                         String type,
                         String exchange,
                         String routingKey,
                         Object payload) {
        String serialized = toJson(payload);
        var event = new OutboxEvent(eventId, aggregateId, aggregateType, type, exchange, routingKey, serialized);
        repository.save(event);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
