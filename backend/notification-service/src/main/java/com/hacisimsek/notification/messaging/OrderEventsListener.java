package com.hacisimsek.notification.messaging;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

import com.hacisimsek.notification.metrics.MetricsService;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsListener {

    private final MetricsService metrics;

    @RabbitListener(queues = "${app.messaging.queues.orderCreated}",
            containerFactory = "manualAckContainerFactory")
    public void onOrderCreated(Map<String, Object> payload,
                               Channel ch,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        log.info("notification: order.created {}", payload);
        metrics.incProcessed("created");
        ch.basicAck(tag, false);
    }

    @RabbitListener(queues = "${app.messaging.queues.orderStatusChanged}",
            containerFactory = "manualAckContainerFactory")
    public void onOrderStatusChanged(Map<String, Object> payload,
                                     Channel ch,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        log.info("📩 notification: order.status-changed {}", payload);
        metrics.incProcessed("status_changed");
        ch.basicAck(tag, false);
    }
}
