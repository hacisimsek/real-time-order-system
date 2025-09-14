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

    // order.created
    @RabbitListener(
            queues = "${app.messaging.queues.orderCreated}",
            containerFactory = "manualAckContainerFactory"
    )
    public void onOrderCreated(Map<String, Object> payload,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        log.info("📩 Received order.created: {}", payload);
        // TODO: e-posta/SMS mock işlemi
        channel.basicAck(tag, false);
    }

    // order.status-changed
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
