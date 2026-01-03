package com.hacisimsek.rtos.inventory.messaging;

import com.hacisimsek.rtos.inventory.metrics.MetricsService;
import com.hacisimsek.rtos.inventory.service.IdempotencyService;
import com.hacisimsek.rtos.inventory.service.InventoryService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final InventoryService service;
    private final IdempotencyService idempotency;
    private final MetricsService metricsService;

    public OrderEventsListener(InventoryService service,
                               IdempotencyService idempotency,
                               MetricsService metricsService) {
        this.service = service;
        this.idempotency = idempotency;
        this.metricsService = metricsService;
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
                var items = (java.util.List<Map<String,Object>>) payload.get("items");
                if (items != null) {
                    for (var it : items) {
                        String sku = String.valueOf(it.get("sku"));
                        Number n = (Number) it.get("qty");
                        int qty = n == null ? 0 : n.intValue();
                        service.reserve(new com.hacisimsek.rtos.inventory.dto.ReserveRequest(sku, qty));
                    }
                }
            });
            metricsService.incProcessed("order.created");
            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("order.created failed (msgId={}, retries={}), payload={}", effectiveId, retries, payload, e);
            metricsService.incFailed("order.created", "exception");
            ch.basicNack(tag, false, false);
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
            var items = (java.util.List<Map<String,Object>>) payload.get("items");
            if (items != null) {
                switch (status) {
                    case "CANCELED" -> {
                        for (var it : items) {
                            String sku = String.valueOf(it.get("sku"));
                            int qty = ((Number) it.get("qty")).intValue();
                            service.release(new com.hacisimsek.rtos.inventory.dto.ReserveRequest(sku, qty));
                        }
                    }
                    case "FULFILLED" -> {
                        for (var it : items) {
                            String sku = String.valueOf(it.get("sku"));
                            int qty = ((Number) it.get("qty")).intValue();
                            service.consume(sku, qty);
                        }
                    }
                    default -> { }
                }
            }
            metricsService.incProcessed("order.status-changed");
            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("order.status-changed failed (msgId={}, retries={}), payload={}", effectiveId, retries, payload, e);
            metricsService.incFailed("order.status-changed", "exception");
            ch.basicNack(tag, false, false);
        }
    }

    private static String firstNonNull(String a, String b, String c) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        if (c != null && !c.isBlank()) return c;
        return null;
    }
}
