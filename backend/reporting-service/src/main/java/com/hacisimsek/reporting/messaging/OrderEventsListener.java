package com.hacisimsek.reporting.messaging;

import com.hacisimsek.reporting.domain.MessageLog;
import com.hacisimsek.reporting.metrics.ReportingMetrics;
import com.hacisimsek.reporting.repository.MessageLogRepository;
import com.hacisimsek.reporting.service.OrderRollupService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final OrderRollupService rollupService;
    private final MessageLogRepository messageLogRepository;
    private final ReportingMetrics metrics;

    public OrderEventsListener(OrderRollupService rollupService,
                               MessageLogRepository messageLogRepository,
                               ReportingMetrics metrics) {
        this.rollupService = rollupService;
        this.messageLogRepository = messageLogRepository;
        this.metrics = metrics;
    }

    @RabbitListener(queues = "${app.messaging.queues.orderCreated}", containerFactory = "manualAckContainerFactory")
    @Transactional
    public void handleOrderCreated(Map<String, Object> payload, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = resolveMessageId(message, payload);

        try {
            if (messageId != null && messageLogRepository.existsById(messageId)) {
                log.debug("Duplicate order event {}, acking", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            long amountCents = Optional.ofNullable(payload.get("amountCents"))
                    .map(Number.class::cast)
                    .map(Number::longValue)
                    .orElse(0L);

            Instant eventInstant = resolveEventInstant(message);
            LocalDate orderDate = eventInstant.atOffset(ZoneOffset.UTC).toLocalDate();
            rollupService.recordOrder(orderDate, amountCents);

            if (messageId != null) {
                messageLogRepository.save(new MessageLog(messageId));
            }

            metrics.recordOrderProcessed(amountCents, eventInstant, Instant.now());

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to process order event message {}", messageId, ex);
            metrics.recordOrderFailed();
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private String resolveMessageId(Message message, Map<String, Object> payload) {
        String messageId = Optional.ofNullable(message.getMessageProperties().getHeader("x-event-id"))
                .map(Object::toString)
                .orElse(null);
        if (messageId == null) {
            messageId = message.getMessageProperties().getMessageId();
        }
        if (messageId == null && payload.containsKey("eventId")) {
            messageId = payload.get("eventId").toString();
        }
        return messageId;
    }

    private Instant resolveEventInstant(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getTimestamp())
                .map(Date::toInstant)
                .orElseGet(Instant::now);
    }
}
