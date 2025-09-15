package com.hacisimsek.notification.messaging;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class OrderEventsListener {

    @RabbitListener(
            queues = "${app.messaging.queues.orderCreated}",
            containerFactory = "manualAckContainerFactory"
    )
    public void onOrderCreated(Map<String,Object> payload, Channel ch, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            log.info("📩 Received order.created: {}", payload);
            if ("C-FAIL".equals(payload.get("customerId"))) {
                throw new IllegalStateException("Simulated processing error");
            }
            ch.basicAck(tag, false);
        } catch (Exception e) {
            log.error("order.created failed -> DLQ", e);
            ch.basicReject(tag, false);
        }
    }

    @RabbitListener(
            queues = "${app.messaging.queues.orderStatusChanged}",
            containerFactory = "manualAckContainerFactory"
    )
    public void onOrderStatusChanged(Map<String, Object> payload,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        log.info("📩 Received order.status-changed: {}", payload);
        channel.basicAck(tag, false);
    }
}
