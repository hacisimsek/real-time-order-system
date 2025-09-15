package com.hacisimsek.inventory.messaging;

import com.hacisimsek.inventory.config.AmqpConfig;
import com.hacisimsek.inventory.service.IdempotencyService;
import com.hacisimsek.inventory.service.InventoryService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);
    private static final int MAX_RETRIES = 3;

    private final InventoryService service;
    private final IdempotencyService idempotency;
    private final RabbitTemplate rabbit;
    private final com.hacisimsek.inventory.config.AmqpProps props;

    public OrderEventsListener(InventoryService service , IdempotencyService idempotency , RabbitTemplate rabbit, com.hacisimsek.inventory.config.AmqpProps props) {
        this.service = service;
        this.idempotency = idempotency;
        this.rabbit = rabbit;
        this.props = props;
    }

    @RabbitListener(queues = "${app.messaging.queues.orderCreated}", containerFactory = "manualAckContainerFactory")
    public void onOrderCreated(Map<String,Object> payload,
                               Channel ch,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                               @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String msgId,
                               @Header(value = "x-event-id", required = false) String xEventId,
                               @Header(value = "x-retries", required = false) Integer retries) throws Exception {
        String effectiveId = firstNonNull(msgId, xEventId, (String) payload.get("eventId"));
        try {
            idempotency.processOnce(effectiveId, () -> {
                @SuppressWarnings("unchecked")
                var items = (java.util.List<java.util.Map<String,Object>>) payload.get("items");
                if (items != null) {
                    for (var it : items) {
                        String sku = String.valueOf(it.get("sku"));
                        int qty = ((Number) it.get("qty")).intValue();
                        service.reserve(new com.hacisimsek.inventory.dto.ReserveRequest(sku, qty));
                    }
                }
            });
            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("order.created failed (msgId={}, retries={})", effectiveId, retries, e);
            handleFailure("order.created", payload, effectiveId, retries, tag, ch,
                    props.routingKeys().orderCreated() + ".retry",
                    props.routingKeys().orderCreated() + ".dlq");
        }
    }

    @RabbitListener(queues = "${app.messaging.queues.orderStatusChanged}", containerFactory = "manualAckContainerFactory")
    public void onOrderStatusChanged(Map<String,Object> payload,
                                     Channel ch,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                                     @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String msgId,
                                     @Header(value = "x-event-id", required = false) String xEventId,
                                     @Header(value = "x-retries", required = false) Integer retries) throws Exception {
        String effectiveId = firstNonNull(msgId, xEventId, (String) payload.get("eventId"));
        try {
            String status = String.valueOf(payload.get("status"));
            @SuppressWarnings("unchecked")
            var items = (java.util.List<java.util.Map<String,Object>>) payload.get("items");
            if (items != null) {
                switch (status) {
                    case "CANCELED" -> {
                        for (var it : items) {
                            String sku = String.valueOf(it.get("sku"));
                            int qty = ((Number) it.get("qty")).intValue();
                            service.release(new com.hacisimsek.inventory.dto.ReserveRequest(sku, qty));
                        }
                    }
                    case "FULFILLED" -> {
                        for (var it : items) {
                            String sku = String.valueOf(it.get("sku"));
                            int qty = ((Number) it.get("qty")).intValue();
                            service.consume(sku, qty);
                        }
                    }
                    default -> {}
                }
            }
            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("status-changed failed (msgId={}, retries={})", effectiveId, retries, e);
            handleFailure("order.status-changed", payload, effectiveId, retries, tag, ch,
                    props.routingKeys().orderStatusChanged() + ".retry",
                    props.routingKeys().orderStatusChanged() + ".dlq");
        }
    }

    private void handleFailure(String which, Map<String,Object> payload, String msgId, Integer retries, long tag, Channel ch, String retryRoutingKey, String dlqRoutingKey) throws Exception {
        int attempt = retries == null ? 0 : retries;
        if (attempt < MAX_RETRIES) {
            int next = attempt + 1;
            log.warn("Retrying {} (attempt {}/{}) messageId={}", which, next, MAX_RETRIES, msgId);
            ch.basicAck(tag, false);
            rabbit.convertAndSend(
                    AmqpConfig.RETRY_EXCHANGE, retryRoutingKey, payload,
                    m -> { m.getMessageProperties().setMessageId(msgId);
                        m.getMessageProperties().setHeader("x-retries", next);
                        return m; });
        } else {
            log.error("Max retries exceeded for {} → send to DLQ, messageId={}", which, msgId);
            ch.basicAck(tag, false);
            rabbit.convertAndSend(
                    AmqpConfig.DLX_EXCHANGE, dlqRoutingKey, payload,
                    m -> { m.getMessageProperties().setMessageId(msgId);
                        m.getMessageProperties().setHeader("x-retries", attempt);
                        return m; });
        }
    }

    private static String firstNonNull(String a, String b, String c) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        if (c != null && !c.isBlank()) return c;
        return null;
    }
}
