package com.hacisimsek.rtos.orders.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacisimsek.rtos.orders.config.OutboxProps;
import com.hacisimsek.rtos.orders.domain.OutboxEvent;
import com.hacisimsek.rtos.orders.domain.OutboxStatus;
import com.hacisimsek.rtos.orders.repository.OutboxEventRepository;
import com.hacisimsek.rtos.orders.metrics.OutboxMetrics;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxProps props;
    private final OutboxMetrics metrics;

    public OutboxRelay(OutboxEventRepository repository,
                       RabbitTemplate rabbitTemplate,
                       ObjectMapper objectMapper,
                       OutboxProps props,
                       OutboxMetrics metrics) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:1s}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = repository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, props.getBatchSize()));
        if (batch.isEmpty()) {
            return;
        }
        batch.forEach(this::publishSingle);
        metrics.refreshPendingCount();
    }

    private void publishSingle(OutboxEvent event) {
        try {
            Map<String, Object> message = objectMapper.readValue(event.getPayload(), MAP_TYPE);
            rabbitTemplate.convertAndSend(event.getExchange(), event.getRoutingKey(), message, msg -> {
                msg.getMessageProperties().setMessageId(event.getEventId());
                msg.getMessageProperties().setHeader("x-event-id", event.getEventId());
                return msg;
            });
            event.markSent();
            log.debug("Dispatched outbox event {}", event.getEventId());
            metrics.recordDispatched(event.getEventType());
        } catch (Exception ex) {
            log.error("Failed to dispatch outbox event {}", event.getEventId(), ex);
            event.markFailed(ex.getMessage(), props.getMaxAttempts());
            metrics.recordFailed(event.getEventType());
        }
    }
}
