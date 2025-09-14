package com.hacisimsek.orders.messaging;

import com.hacisimsek.orders.config.MessagingProps;
import com.hacisimsek.orders.domain.OrderItem;
import com.hacisimsek.orders.domain.OrderStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private final RabbitTemplate rabbit;
    private final MessagingProps props;

    public RabbitOrderEventPublisher(RabbitTemplate rabbit, MessagingProps props) {
        this.rabbit = rabbit;
        this.props = props;
    }

    @Override
    public void orderCreated(Long id, String customerId, long amountCents, String currency, List<OrderItem> items) {
        var itemsList = items.stream()
                .map(i -> Map.of("sku", i.getSku(), "qty", i.getQty()))
                .toList();
        rabbit.convertAndSend(
                props.exchange(),
                props.routingKeys().orderCreated(),
                Map.of(
                        "type", "order.created.v1",
                        "id", id,
                        "customerId", customerId,
                        "amountCents", amountCents,
                        "currency", currency,
                        "items", itemsList
                )
        );
    }

    @Override
    public void orderStatusChanged(Long id, OrderStatus status, List<OrderItem> items) {
        var itemsList = items.stream()
                .map(i -> Map.of("sku", i.getSku(), "qty", i.getQty()))
                .toList();
        rabbit.convertAndSend(
                props.exchange(),
                props.routingKeys().orderStatusChanged(),
                Map.of(
                        "type", "order.status-changed.v1",
                        "id", id,
                        "status", status.name(),
                        "items", itemsList
                )
        );
    }
}
