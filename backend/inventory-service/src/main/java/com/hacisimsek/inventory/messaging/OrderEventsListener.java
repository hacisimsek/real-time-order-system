package com.hacisimsek.inventory.messaging;

import com.hacisimsek.inventory.dto.ReserveRequest;
import com.hacisimsek.inventory.service.InventoryService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);
    private final InventoryService service;

    public OrderEventsListener(InventoryService service) {
        this.service = service;
    }

    // order.created -> reserve
    @RabbitListener(queues = "${app.messaging.queues.orderCreated}", containerFactory = "manualAckContainerFactory")
    public void onOrderCreated(Map<String, Object> payload,
                               Channel ch,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            log.info("📦 Reserve on order.created: {}", payload);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            if (items != null) {
                for (var it : items) {
                    String sku = String.valueOf(it.get("sku"));
                    int qty = ((Number) it.get("qty")).intValue();
                    service.reserve(new ReserveRequest(sku, qty));
                }
            }
            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("❌ reserve failed", e);
            ch.basicReject(tag, false);
        }
    }

    // order.status-changed -> CANCELED: release, FULFILLED: consume
    @RabbitListener(queues = "${app.messaging.queues.orderStatusChanged}", containerFactory = "manualAckContainerFactory")
    public void onOrderStatusChanged(Map<String, Object> payload,
                                     Channel ch,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            String status = String.valueOf(payload.get("status"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            if (items != null) {
                switch (status) {
                    case "CANCELED" -> {
                        for (var it : items) {
                            String sku = String.valueOf(it.get("sku"));
                            int qty = ((Number) it.get("qty")).intValue();
                            service.release(new ReserveRequest(sku, qty));
                        }
                    }
                    case "FULFILLED" -> {
                        for (var it : items) {
                            String sku = String.valueOf(it.get("sku"));
                            int qty = ((Number) it.get("qty")).intValue();
                            service.consume(sku, qty); // reserved -= qty
                        }
                    }
                    default -> log.info("Status {} -> no inventory action", status);
                }
            }

            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("❌ status handling failed", e);
            ch.basicReject(tag, false);
        }
    }
}
