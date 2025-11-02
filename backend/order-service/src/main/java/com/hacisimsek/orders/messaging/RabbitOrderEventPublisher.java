package com.hacisimsek.orders.messaging;

import com.hacisimsek.orders.config.MessagingProps;
import com.hacisimsek.orders.domain.OrderStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private final RabbitTemplate rabbit;
    private final MessagingProps props;

    public RabbitOrderEventPublisher(RabbitTemplate rabbit, MessagingProps props) {
        this.rabbit = rabbit;
        this.props = props;
    }

    @Override
    public void orderCreated(Long id, String customerId, long amountCents, String currency, List<OrderEventItem> items) {
        var eventId = UUID.randomUUID().toString();
        var itemsList = items.stream().map(i -> Map.of("sku", i.sku(), "qty", i.qty())).toList();

        var payload = Map.of(
                "type","order.created.v1",
                "eventId", eventId,
                "id", id,
                "customerId", customerId,
                "amountCents", amountCents,
                "currency", currency,
                "items", itemsList
        );

        rabbit.convertAndSend(props.exchange(), props.routingKeys().orderCreated(), payload, msg -> {
            msg.getMessageProperties().setMessageId(eventId);  // <-- AMQP Message ID
            msg.getMessageProperties().setHeader("x-event-id", eventId); // opsiyonel header
            return msg;
        });
    }

    @Override
    public void orderStatusChanged(Long id, OrderStatus status, List<OrderEventItem> items) {
        var eventId = UUID.randomUUID().toString();
        var itemsList = items.stream().map(i -> Map.of("sku", i.sku(), "qty", i.qty())).toList();

        var payload = Map.of(
                "type","order.status-changed.v1",
                "eventId", eventId,
                "id", id,
                "status", status.name(),
                "items", itemsList
        );

        rabbit.convertAndSend(props.exchange(), props.routingKeys().orderStatusChanged(), payload, msg -> {
            msg.getMessageProperties().setMessageId(eventId);
            msg.getMessageProperties().setHeader("x-event-id", eventId);
            return msg;
        });
    }
}
